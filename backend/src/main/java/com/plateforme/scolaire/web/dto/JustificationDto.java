package com.plateforme.scolaire.web.dto;

import com.plateforme.scolaire.domain.JustificationAbsence;
import java.time.Instant;
import java.time.LocalDate;

public record JustificationDto(Long id, Long presenceId, Long eleveId, String eleve, String classe,
                               LocalDate dateAbsence, String typePresence, String motif, String statut,
                               String reponse, Instant soumiseLe) {

    public static JustificationDto de(JustificationAbsence j) {
        var p = j.getPresence();
        var i = p.getInscription();
        var e = i.getEleve();
        return new JustificationDto(j.getId(), p.getId(), e.getId(), (e.getPrenom() + " " + e.getNom()).trim(),
                i.getClasse() == null ? null : i.getClasse().getNom(), p.getDateSeance(), p.getStatut(), j.getMotif(),
                j.getStatut(), j.getReponse(), j.getCreatedAt());
    }
}
