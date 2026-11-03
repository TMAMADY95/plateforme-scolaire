package com.plateforme.scolaire.service;

import com.plateforme.scolaire.repository.CandidatExamenRepository;
import com.plateforme.scolaire.security.PerimetreService;
import com.plateforme.scolaire.web.dto.AmenagementCandidatRequest;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Déclaration du handicap d'un candidat et de l'aménagement accordé (donnée de santé : seuls le type et l'aménagement sont conservés). */
@Service
@RequiredArgsConstructor
@Transactional
public class AmenagementCandidatService {

    static final Set<String> HANDICAPS = Set.of("MOTEUR", "VISUEL", "AUDITIF", "INTELLECTUEL", "AUTRE");

    private final CandidatExamenRepository candidats;
    private final PerimetreService perimetre;
    private final AuditService audit;

    /** Pur : type de handicap normalisé ; null si vide. */
    static String normaliser(String handicap) {
        return handicap == null || handicap.isBlank() ? null : handicap.trim().toUpperCase();
    }

    public void definir(Long candidatId, AmenagementCandidatRequest q) {
        var c = candidats.findById(candidatId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Candidat introuvable"));
        if (c.getEtablissement() == null || !perimetre.peutGererEtablissement(c.getEtablissement().getId())) {
            throw new AccessDeniedException("Candidat hors de votre établissement");
        }
        String handicap = normaliser(q.handicap());
        if (handicap != null && !HANDICAPS.contains(handicap)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Type de handicap inconnu : " + q.handicap());
        }
        c.setHandicap(handicap);
        c.setAmenagement(handicap == null || q.amenagement() == null || q.amenagement().isBlank() ? null : q.amenagement().trim());
        audit.enregistrer("CANDIDAT_AMENAGEMENT", "ExamenNational", c.getExamen().getId(), Map.of("candidat", candidatId, "declare", handicap != null));
    }
}
