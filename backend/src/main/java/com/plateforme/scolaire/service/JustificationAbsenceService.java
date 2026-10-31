package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.JustificationAbsence;
import com.plateforme.scolaire.repository.JustificationAbsenceRepository;
import com.plateforme.scolaire.repository.PresenceRepository;
import com.plateforme.scolaire.repository.UtilisateurRepository;
import com.plateforme.scolaire.security.PerimetreService;
import com.plateforme.scolaire.web.dto.DecisionJustificationRequest;
import com.plateforme.scolaire.web.dto.JustificationDto;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Les familles demandent en ligne la justification d'une absence ; la direction de l'école accepte ou refuse. */
@Service
@RequiredArgsConstructor
@Transactional
public class JustificationAbsenceService {

    private final JustificationAbsenceRepository justifications;
    private final PresenceRepository presences;
    private final UtilisateurRepository utilisateurs;
    private final PerimetreService perimetre;
    private final AuditService audit;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<JustificationDto> deLEleve(Long eleveId) {
        return justifications.deLEleve(eleveId).stream().map(JustificationDto::de).toList();
    }

    @Transactional(readOnly = true)
    public List<JustificationDto> enAttente(Long etablissementId) {
        return justifications.enAttenteDeLEtablissement(etablissementId).stream().map(JustificationDto::de).toList();
    }

    public JustificationDto soumettre(Long presenceId, String motif) {
        var presence = presences.findById(presenceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Présence introuvable"));
        Long eleveId = presence.getInscription().getEleve().getId();
        if (!perimetre.peutAccederEleve(eleveId)) {
            throw new AccessDeniedException("Cet élève n'est pas rattaché à votre compte");
        }
        if ("PRESENT".equals(presence.getStatut())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rien à justifier : l'élève était présent");
        }
        if (presence.isJustifie()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette absence est déjà justifiée");
        }
        if (justifications.existsByPresenceIdAndStatut(presenceId, "EN_ATTENTE")) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Une demande est déjà en cours pour cette absence");
        }
        JustificationAbsence j = new JustificationAbsence();
        j.setPresence(presence);
        j.setSoumisePar(utilisateurs.getReferenceById(perimetre.utilisateurCourant().getId()));
        j.setMotif(motif.trim());
        j.setCreatedAt(Instant.now(clock));
        JustificationAbsence enregistree = justifications.save(j);
        audit.enregistrer("JUSTIFICATION_DEMANDEE", "JustificationAbsence", enregistree.getId(),
                Map.of("presence", presenceId));
        return justifications.deLEleve(eleveId).stream().filter(x -> x.getId().equals(enregistree.getId()))
                .findFirst().map(JustificationDto::de).orElseThrow();
    }

    public JustificationDto decider(Long id, DecisionJustificationRequest r) {
        JustificationAbsence j = justifications.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demande introuvable"));
        var presence = j.getPresence();
        Long etablissementId = presence.getInscription().getEtablissement().getId();
        if (!perimetre.peutGererEtablissement(etablissementId)) {
            throw new AccessDeniedException("Demande hors de votre établissement");
        }
        if (!"EN_ATTENTE".equals(j.getStatut())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette demande a déjà été traitée");
        }
        j.setStatut(r.accepter() ? "ACCEPTEE" : "REFUSEE");
        j.setReponse(r.reponse() == null || r.reponse().isBlank() ? null : r.reponse().trim());
        j.setTraiteeLe(Instant.now(clock));
        j.setTraiteePar(utilisateurs.getReferenceById(perimetre.utilisateurCourant().getId()));
        if (r.accepter()) {
            presence.setJustifie(true);
            if (presence.getMotif() == null || presence.getMotif().isBlank()) {
                presence.setMotif(j.getMotif().length() > 255 ? j.getMotif().substring(0, 255) : j.getMotif());
            }
        }
        audit.enregistrer("JUSTIFICATION_" + j.getStatut(), "JustificationAbsence", j.getId(),
                Map.of("presence", presence.getId()));
        return JustificationDto.de(j);
    }
}
