package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.CandidatExamen;
import com.plateforme.scolaire.domain.DiplomeExamen;
import com.plateforme.scolaire.domain.Etablissement;
import com.plateforme.scolaire.domain.ExamenNational;
import com.plateforme.scolaire.repository.DiplomeExamenRepository;
import com.plateforme.scolaire.repository.EtablissementRepository;
import com.plateforme.scolaire.repository.ExamenNationalRepository;
import com.plateforme.scolaire.web.dto.DiplomesExamenDto;
import com.plateforme.scolaire.web.dto.VerificationDiplomeDto;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Diplômes : après la proclamation, le ministère délivre un diplôme à chaque candidat admis. Chaque diplôme porte un numéro et un
 * code de vérification aléatoire ; n'importe qui peut contrôler en ligne qu'un diplôme est authentique, ou révoqué (par exemple
 * après la découverte d'une fraude). La mention se déduit de la moyenne.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class DiplomesExamenService {

    /** Sans 0, O, 1, I, L : les codes se recopient sans ambiguïté. */
    static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    static final int LONGUEUR_CODE = 10;

    private final DiplomeExamenRepository diplomes;
    private final ExamenNationalRepository examens;
    private final EtablissementRepository etablissements;
    private final GenerateurDiplomesPdf pdf;
    private final AuditService audit;
    private final Clock clock;
    private final RandomGenerator hasard = new SecureRandom();

    /** Mention selon la moyenne sur 20 : 16 très bien, 14 bien, 12 assez bien, sinon passable. */
    static String mention(BigDecimal moyenne) {
        if (moyenne == null) {
            return "PASSABLE";
        }
        if (moyenne.compareTo(BigDecimal.valueOf(16)) >= 0) {
            return "TRES_BIEN";
        }
        if (moyenne.compareTo(BigDecimal.valueOf(14)) >= 0) {
            return "BIEN";
        }
        return moyenne.compareTo(BigDecimal.valueOf(12)) >= 0 ? "ASSEZ_BIEN" : "PASSABLE";
    }

    static String genererCode(RandomGenerator hasard) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < LONGUEUR_CODE; i++) {
            s.append(ALPHABET.charAt(hasard.nextInt(ALPHABET.length())));
        }
        return s.toString();
    }

    /** Ramène un code saisi à sa forme stockée : majuscules, sans espaces ni tirets. */
    static String normaliser(String saisie) {
        return saisie == null ? "" : saisie.toUpperCase().replaceAll("[^A-Z0-9]", "");
    }

    static String numero(Long examenId, Long candidatId) {
        return String.format("DIP-%d-%06d", examenId, candidatId);
    }

    @Transactional(readOnly = true)
    public DiplomesExamenDto diplomes(Long examenId) {
        exigerMinistere("Les diplômes relèvent du ministère");
        return dto(examen(examenId));
    }

    /** Délivre un diplôme à chaque admis qui n'en a pas encore : possible seulement après la proclamation. */
    public DiplomesExamenDto delivrer(Long examenId) {
        exigerMinistere("Seul le ministère délivre les diplômes");
        ExamenNational examen = examen(examenId);
        if (!"PROCLAME".equals(examen.getStatutResultats())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Les diplômes sont délivrés après la proclamation des résultats");
        }
        int nouveaux = 0;
        for (CandidatExamen c : diplomes.admisSansDiplome(examenId)) {
            DiplomeExamen d = new DiplomeExamen();
            d.setCandidat(c);
            d.setNumero(numero(examenId, c.getId()));
            d.setCode(codeUnique());
            d.setMention(mention(c.getMoyenne()));
            d.setDelivreLe(LocalDate.now(clock));
            d.setCreatedAt(Instant.now(clock));
            diplomes.save(d);
            nouveaux++;
        }
        audit.enregistrer("DIPLOMES_DELIVRES", "ExamenNational", examenId, Map.of("nombre", nouveaux));
        return dto(examen);
    }

    public DiplomesExamenDto revoquer(Long examenId, Long diplomeId, String motif) {
        exigerMinistere("Seul le ministère révoque un diplôme");
        ExamenNational examen = examen(examenId);
        DiplomeExamen d = diplomes.findById(diplomeId).filter(x -> x.getCandidat().getExamen().getId().equals(examenId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Diplôme introuvable"));
        if (motif == null || motif.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indiquez le motif de la révocation");
        }
        if (d.isRevoque()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce diplôme est déjà révoqué");
        }
        d.setRevoque(true);
        d.setMotifRevocation(motif.trim().length() > 500 ? motif.trim().substring(0, 500) : motif.trim());
        d.setRevoqueLe(Instant.now(clock));
        audit.enregistrer("DIPLOME_REVOQUE", "ExamenNational", examenId, Map.of("numero", d.getNumero()));
        return dto(examen);
    }

    /** Vérification publique : aucune information n'est donnée sur un code inconnu. */
    @Transactional(readOnly = true)
    public VerificationDiplomeDto verifier(String saisie) {
        String code = normaliser(saisie);
        if (code.length() != LONGUEUR_CODE) {
            return VerificationDiplomeDto.inconnu();
        }
        return diplomes.parCode(code).map(d -> {
            var e = d.getCandidat().getEleve();
            return new VerificationDiplomeDto(d.isRevoque() ? "REVOQUE" : "AUTHENTIQUE", (e.getPrenom() + " " + e.getNom()).trim(),
                    d.getCandidat().getExamen().getLibelle(), d.getCandidat().getExamen().getAnneeScolaire().getLibelle(),
                    d.getMention(), d.getNumero(), d.getDelivreLe(), d.isRevoque() ? d.getMotifRevocation() : null);
        }).orElseGet(VerificationDiplomeDto::inconnu);
    }

    /** Diplômes valables d'un établissement, en PDF (accès contrôlé par le périmètre dans le contrôleur). */
    @Transactional(readOnly = true)
    public byte[] pdf(Long etablissementId, Long examenId) {
        ExamenNational examen = examen(examenId);
        Etablissement etab = etablissements.findById(etablissementId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Établissement introuvable"));
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        List<GenerateurDiplomesPdf.Diplome> liste = diplomes.valablesDeLEtablissement(examenId, etablissementId).stream()
                .map(d -> new GenerateurDiplomesPdf.Diplome((d.getCandidat().getEleve().getPrenom() + " " + d.getCandidat().getEleve().getNom()).trim(),
                        d.getCandidat().getEleve().getMatricule(), d.getNumero(), d.getCode(), d.getMention(), d.getDelivreLe().format(fmt)))
                .toList();
        return pdf.diplomes(new GenerateurDiplomesPdf.DonneesDiplomes(etab.getNom(), examen.getLibelle(), examen.getAnneeScolaire().getLibelle(), liste));
    }

    private String codeUnique() {
        for (int i = 0; i < 20; i++) {
            String code = genererCode(hasard);
            if (!diplomes.existsByCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Impossible de générer un code de vérification unique");
    }

    private DiplomesExamenDto dto(ExamenNational examen) {
        List<DiplomeExamen> liste = diplomes.deLExamen(examen.getId());
        long revoques = liste.stream().filter(DiplomeExamen::isRevoque).count();
        return new DiplomesExamenDto(examen.getId(), examen.getStatutResultats(), diplomes.countAdmis(examen.getId()), liste.size(), revoques,
                liste.stream().map(d -> new DiplomesExamenDto.Diplome(d.getId(), d.getNumero(),
                        (d.getCandidat().getEleve().getPrenom() + " " + d.getCandidat().getEleve().getNom()).trim(),
                        d.getCandidat().getEtablissement().getNom(), d.getMention(), d.getDelivreLe(), d.isRevoque(), d.getMotifRevocation())).toList());
    }

    private static void exigerMinistere(String message) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities().stream().noneMatch(a -> "ROLE_MINISTERE".equals(a.getAuthority()))) {
            throw new AccessDeniedException(message);
        }
    }

    private ExamenNational examen(Long id) {
        return examens.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Examen introuvable"));
    }
}
