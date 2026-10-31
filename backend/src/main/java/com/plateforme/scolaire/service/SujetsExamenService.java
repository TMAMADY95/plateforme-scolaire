package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.EpreuveExamen;
import com.plateforme.scolaire.domain.ExamenNational;
import com.plateforme.scolaire.domain.SujetExamen;
import com.plateforme.scolaire.repository.EpreuveExamenRepository;
import com.plateforme.scolaire.repository.ExamenNationalRepository;
import com.plateforme.scolaire.repository.SujetExamenRepository;
import com.plateforme.scolaire.web.dto.BanqueSujetsDto;
import com.plateforme.scolaire.web.dto.SujetExamenRequest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Banque de sujets : chaque épreuve a trois variantes (A, B, C) élaborées puis validées. Le tirage au sort d'une variante
 * n'est possible que lorsque les trois sont validées ; en cas de fuite, la variante utilisée est déclarée compromise et
 * remplacée par tirage parmi les variantes de réserve. Seuls le suivi des sujets est enregistré, pas leur contenu.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class SujetsExamenService {

    static final Set<String> VARIANTES = Set.of("A", "B", "C");
    static final int VARIANTES_REQUISES = 3;

    private final SujetExamenRepository sujets;
    private final EpreuveExamenRepository epreuves;
    private final ExamenNationalRepository examens;
    private final AuditService audit;
    private final Clock clock;
    private final RandomGenerator hasard = new SecureRandom();

    /** Variante tirée au sort parmi les candidates (jamais vide). */
    static <T> T choisir(List<T> candidates, RandomGenerator hasard) {
        return candidates.get(hasard.nextInt(candidates.size()));
    }

    @Transactional(readOnly = true)
    public BanqueSujetsDto banque(Long examenId) {
        ExamenNational examen = examen(examenId);
        verifierDroit(examen);
        return dto(examen);
    }

    public BanqueSujetsDto ajouter(Long examenId, Long epreuveId, SujetExamenRequest r) {
        ExamenNational examen = examen(examenId);
        verifierDroit(examen);
        EpreuveExamen epreuve = epreuve(examenId, epreuveId);
        String variante = r.variante().trim().toUpperCase();
        if (!VARIANTES.contains(variante)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La variante doit être A, B ou C");
        }
        List<SujetExamen> existants = sujets.findByEpreuveIdOrderByVariante(epreuveId);
        if (existants.stream().anyMatch(s -> s.getVariante().equals(variante))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La variante " + variante + " existe déjà pour cette épreuve");
        }
        if (existants.stream().anyMatch(s -> "TIRE".equals(s.getStatut()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Un sujet a déjà été tiré pour cette épreuve");
        }
        SujetExamen s = new SujetExamen();
        s.setEpreuve(epreuve);
        s.setVariante(variante);
        s.setIntitule(r.intitule().trim());
        s.setAuteurs(r.auteurs() == null || r.auteurs().isBlank() ? null : r.auteurs().trim());
        s.setCreatedAt(Instant.now(clock));
        sujets.save(s);
        audit.enregistrer("SUJET_EXAMEN_AJOUTE", "ExamenNational", examenId, Map.of("epreuve", epreuveId, "variante", variante));
        return dto(examen);
    }

    public BanqueSujetsDto supprimer(Long examenId, Long sujetId) {
        ExamenNational examen = examen(examenId);
        verifierDroit(examen);
        SujetExamen s = sujet(examenId, sujetId);
        if (!"ELABORATION".equals(s.getStatut()) && !"VALIDE".equals(s.getStatut())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Un sujet tiré ou compromis ne peut plus être supprimé");
        }
        sujets.delete(s);
        sujets.flush();
        audit.enregistrer("SUJET_EXAMEN_SUPPRIME", "ExamenNational", examenId, Map.of("variante", s.getVariante()));
        return dto(examen);
    }

    public BanqueSujetsDto valider(Long examenId, Long sujetId) {
        ExamenNational examen = examen(examenId);
        verifierDroit(examen);
        SujetExamen s = sujet(examenId, sujetId);
        if (!"ELABORATION".equals(s.getStatut())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce sujet est déjà validé");
        }
        s.setStatut("VALIDE");
        s.setValideLe(Instant.now(clock));
        audit.enregistrer("SUJET_EXAMEN_VALIDE", "ExamenNational", examenId, Map.of("variante", s.getVariante()));
        return dto(examen);
    }

    /** Tirage au sort de la variante utilisée : les trois variantes doivent être validées. */
    public BanqueSujetsDto tirer(Long examenId, Long epreuveId) {
        ExamenNational examen = examen(examenId);
        verifierDroit(examen);
        epreuve(examenId, epreuveId);
        List<SujetExamen> liste = sujets.findByEpreuveIdOrderByVariante(epreuveId);
        if (liste.stream().anyMatch(s -> "TIRE".equals(s.getStatut()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Un sujet a déjà été tiré pour cette épreuve");
        }
        if (liste.stream().anyMatch(s -> "COMPROMIS".equals(s.getStatut()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Un sujet compromis a déjà été remplacé ou doit l'être par un tirage de réserve");
        }
        long validees = liste.stream().filter(s -> "VALIDE".equals(s.getStatut())).count();
        if (validees < VARIANTES_REQUISES) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Les trois variantes doivent être validées avant le tirage (" + validees + " sur " + VARIANTES_REQUISES + ")");
        }
        tirerParmiLaReserve(liste);
        audit.enregistrer("SUJET_EXAMEN_TIRE", "ExamenNational", examenId, Map.of("epreuve", epreuveId));
        return dto(examen);
    }

    /** Fuite constatée : la variante utilisée est compromise et remplacée par une variante de réserve. */
    public BanqueSujetsDto compromettre(Long examenId, Long sujetId) {
        ExamenNational examen = examen(examenId);
        verifierDroit(examen);
        SujetExamen s = sujet(examenId, sujetId);
        if (!"TIRE".equals(s.getStatut())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Seul le sujet utilisé peut être déclaré compromis");
        }
        List<SujetExamen> liste = sujets.findByEpreuveIdOrderByVariante(s.getEpreuve().getId());
        if (liste.stream().noneMatch(x -> "VALIDE".equals(x.getStatut()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Aucune variante de réserve : élaborez et validez un nouveau sujet avant de déclarer la fuite");
        }
        s.setStatut("COMPROMIS");
        tirerParmiLaReserve(liste);
        audit.enregistrer("SUJET_EXAMEN_COMPROMIS", "ExamenNational", examenId, Map.of("variante", s.getVariante()));
        return dto(examen);
    }

    private void tirerParmiLaReserve(List<SujetExamen> liste) {
        SujetExamen choisi = choisir(liste.stream().filter(x -> "VALIDE".equals(x.getStatut())).toList(), hasard);
        choisi.setStatut("TIRE");
        choisi.setTireLe(Instant.now(clock));
    }

    private BanqueSujetsDto dto(ExamenNational examen) {
        Map<Long, List<SujetExamen>> parEpreuve = sujets.deLExamen(examen.getId()).stream()
                .collect(Collectors.groupingBy(s -> s.getEpreuve().getId()));
        return new BanqueSujetsDto(examen.getId(), epreuves.findByExamenIdOrderByDateEpreuveAscHeureDebutAsc(examen.getId()).stream().map(e -> {
            List<SujetExamen> liste = parEpreuve.getOrDefault(e.getId(), List.of());
            long validees = liste.stream().filter(s -> "VALIDE".equals(s.getStatut()) || "TIRE".equals(s.getStatut())).count();
            String tiree = liste.stream().filter(s -> "TIRE".equals(s.getStatut())).map(SujetExamen::getVariante).findFirst().orElse(null);
            boolean tirable = tiree == null && liste.stream().filter(s -> "VALIDE".equals(s.getStatut())).count() >= VARIANTES_REQUISES
                    && liste.stream().noneMatch(s -> "COMPROMIS".equals(s.getStatut()));
            return new BanqueSujetsDto.EpreuveSujets(e.getId(), e.nom(), e.getDateEpreuve(),
                    liste.stream().map(s -> new BanqueSujetsDto.Sujet(s.getId(), s.getVariante(), s.getIntitule(), s.getAuteurs(),
                            s.getStatut(), s.getValideLe(), s.getTireLe())).toList(),
                    (int) validees, tirable, tiree);
        }).toList());
    }

    private EpreuveExamen epreuve(Long examenId, Long epreuveId) {
        return epreuves.findById(epreuveId).filter(e -> e.getExamen().getId().equals(examenId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Épreuve inconnue pour cet examen"));
    }

    private SujetExamen sujet(Long examenId, Long sujetId) {
        return sujets.findById(sujetId).filter(s -> s.getEpreuve().getExamen().getId().equals(examenId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sujet introuvable"));
    }

    /** Le ministère gère toutes les banques ; une direction régionale seulement celles des examens non nationaux. */
    private void verifierDroit(ExamenNational examen) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        boolean ministere = auth != null && auth.getAuthorities().stream().anyMatch(a -> "ROLE_MINISTERE".equals(a.getAuthority()));
        if (!ministere && "NATIONAL".equalsIgnoreCase(examen.getNiveauOrganisation())) {
            throw new AccessDeniedException("Les sujets d'un examen national relèvent du ministère");
        }
    }

    private ExamenNational examen(Long id) {
        return examens.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Examen introuvable"));
    }
}
