package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.CandidatExamen;
import com.plateforme.scolaire.domain.ExamenNational;
import com.plateforme.scolaire.domain.ListeCandidats;
import com.plateforme.scolaire.domain.PieceCandidature;
import com.plateforme.scolaire.repository.CandidatExamenRepository;
import com.plateforme.scolaire.repository.EtablissementRepository;
import com.plateforme.scolaire.repository.ExamenNationalRepository;
import com.plateforme.scolaire.repository.ListeCandidatsRepository;
import com.plateforme.scolaire.repository.PieceCandidatureRepository;
import com.plateforme.scolaire.web.dto.ListeCandidatsDto;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Circuit de validation des listes de candidats (règlement général) : l'établissement établit et transmet sa liste, l'autorité
 * régionale la vérifie, puis le ministère (DGECS) valide la liste définitive. Chaque niveau peut rejeter la liste avec un motif :
 * elle revient alors à l'établissement. Une liste transmise n'est plus modifiable.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ListeCandidatsService {

    static final String BROUILLON = "BROUILLON";
    static final String TRANSMISE = "TRANSMISE";
    static final String VALIDEE_REGION = "VALIDEE_REGION";
    static final String VALIDEE = "VALIDEE";

    private final ListeCandidatsRepository listes;
    private final CandidatExamenRepository candidats;
    private final PieceCandidatureRepository pieces;
    private final ExamenNationalRepository examens;
    private final EtablissementRepository etablissements;
    private final AuditService audit;
    private final Clock clock;

    @Transactional(readOnly = true)
    public ListeCandidatsDto liste(Long etablissementId, Long examenId) {
        ExamenNational examen = examen(examenId);
        return dto(listes.findByExamenIdAndEtablissementId(examenId, etablissementId).orElse(null), examen, etablissementId);
    }

    /** L'établissement transmet sa liste : au moins un candidat et tous les dossiers complets. */
    public ListeCandidatsDto transmettre(Long etablissementId, Long examenId) {
        ExamenNational examen = examen(examenId);
        ListeCandidats l = obtenir(examen, etablissementId);
        if (!BROUILLON.equals(l.getStatut())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La liste a déjà été transmise");
        }
        List<CandidatExamen> liste = candidats.deLEtablissement(examenId, etablissementId);
        if (liste.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Aucun candidat n'est inscrit : il n'y a rien à transmettre");
        }
        int incomplets = liste.size() - completsDe(examen, liste);
        if (incomplets > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, incomplets + " dossier(s) de candidature incomplet(s) : complétez-les avant de transmettre la liste");
        }
        l.setStatut(TRANSMISE);
        l.setTransmiseLe(Instant.now(clock));
        l.setMotifRejet(null);
        audit.enregistrer("LISTE_CANDIDATS_TRANSMISE", "ExamenNational", examenId, Map.of("etablissement", etablissementId, "candidats", liste.size()));
        return dto(l, examen, etablissementId);
    }

    /** Direction régionale ou ministère : la liste transmise est vérifiée. */
    public ListeCandidatsDto validerRegion(Long etablissementId, Long examenId) {
        ExamenNational examen = examen(examenId);
        ListeCandidats l = obtenir(examen, etablissementId);
        if (!TRANSMISE.equals(l.getStatut())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Seule une liste transmise peut être vérifiée");
        }
        l.setStatut(VALIDEE_REGION);
        l.setValideeRegionLe(Instant.now(clock));
        audit.enregistrer("LISTE_CANDIDATS_VERIFIEE", "ExamenNational", examenId, Map.of("etablissement", etablissementId));
        return dto(l, examen, etablissementId);
    }

    /** Ministère : validation de la liste définitive, après vérification régionale. */
    public ListeCandidatsDto validerNationale(Long etablissementId, Long examenId) {
        ExamenNational examen = examen(examenId);
        if (!estMinistere()) {
            throw new AccessDeniedException("Seul le ministère valide les listes définitives");
        }
        ListeCandidats l = obtenir(examen, etablissementId);
        if (!VALIDEE_REGION.equals(l.getStatut())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La liste doit d'abord être vérifiée par la direction régionale");
        }
        l.setStatut(VALIDEE);
        l.setValideeLe(Instant.now(clock));
        audit.enregistrer("LISTE_CANDIDATS_VALIDEE", "ExamenNational", examenId, Map.of("etablissement", etablissementId));
        return dto(l, examen, etablissementId);
    }

    /** Renvoie la liste à l'établissement avec un motif. Une liste déjà vérifiée par la région ne se rejette qu'au ministère. */
    public ListeCandidatsDto rejeter(Long etablissementId, Long examenId, String motif) {
        ExamenNational examen = examen(examenId);
        ListeCandidats l = obtenir(examen, etablissementId);
        if (motif == null || motif.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indiquez le motif du rejet");
        }
        boolean rejetable = TRANSMISE.equals(l.getStatut()) || (VALIDEE_REGION.equals(l.getStatut()) && estMinistere());
        if (!rejetable) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, VALIDEE.equals(l.getStatut())
                    ? "La liste est définitivement validée" : "Cette liste ne peut pas être rejetée dans son état actuel");
        }
        l.setStatut(BROUILLON);
        l.setTransmiseLe(null);
        l.setValideeRegionLe(null);
        l.setMotifRejet(motif.trim().length() > 500 ? motif.trim().substring(0, 500) : motif.trim());
        audit.enregistrer("LISTE_CANDIDATS_REJETEE", "ExamenNational", examenId, Map.of("etablissement", etablissementId));
        return dto(l, examen, etablissementId);
    }

    /** Vrai quand la liste n'est plus modifiable par l'établissement (transmise ou validée). */
    @Transactional(readOnly = true)
    public boolean verrouillee(Long examenId, Long etablissementId) {
        return listes.findByExamenIdAndEtablissementId(examenId, etablissementId)
                .map(l -> !BROUILLON.equals(l.getStatut())).orElse(false);
    }

    private ListeCandidats obtenir(ExamenNational examen, Long etablissementId) {
        return listes.findByExamenIdAndEtablissementId(examen.getId(), etablissementId).orElseGet(() -> {
            ListeCandidats l = new ListeCandidats();
            l.setExamen(examen);
            l.setEtablissement(etablissements.findById(etablissementId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Établissement introuvable")));
            return listes.save(l);
        });
    }

    private int completsDe(ExamenNational examen, List<CandidatExamen> liste) {
        Map<Long, Set<String>> fournies = pieces.findByCandidatIdIn(liste.stream().map(CandidatExamen::getId).toList()).stream()
                .collect(Collectors.groupingBy(p -> p.getCandidat().getId(), Collectors.mapping(PieceCandidature::getType, Collectors.toSet())));
        return (int) liste.stream()
                .filter(c -> DossierCandidatureService.manques(examen.getNiveauOrganisation(), fournies.getOrDefault(c.getId(), Set.of()), c.isLibre()).isEmpty())
                .count();
    }

    private ListeCandidatsDto dto(ListeCandidats l, ExamenNational examen, Long etablissementId) {
        List<CandidatExamen> liste = candidats.deLEtablissement(examen.getId(), etablissementId);
        int complets = liste.isEmpty() ? 0 : completsDe(examen, liste);
        return new ListeCandidatsDto(l == null ? BROUILLON : l.getStatut(), liste.size(), complets,
                l == null ? null : l.getTransmiseLe(), l == null ? null : l.getValideeRegionLe(),
                l == null ? null : l.getValideeLe(), l == null ? null : l.getMotifRejet());
    }

    private static boolean estMinistere() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream().anyMatch(a -> "ROLE_MINISTERE".equals(a.getAuthority()));
    }

    private ExamenNational examen(Long id) {
        return examens.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Examen introuvable"));
    }
}
