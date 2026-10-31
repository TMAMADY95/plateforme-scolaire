package com.plateforme.scolaire.web.dto;

import com.plateforme.scolaire.domain.PlanAction;
import java.time.Instant;
import java.time.LocalDate;

public record PlanActionDto(Long id, Long etablissementId, String etablissement, String zone, String description,
                            LocalDate echeance, String statut, boolean enRetard, String note, String creePar,
                            Instant createdAt, Instant termineLe) {

    public static PlanActionDto de(PlanAction p, LocalDate aujourdhui) {
        var e = p.getEtablissement();
        var u = p.getCreePar();
        boolean ouvert = "A_FAIRE".equals(p.getStatut()) || "EN_COURS".equals(p.getStatut());
        return new PlanActionDto(p.getId(), e.getId(), e.getNom(), e.getZone() == null ? null : e.getZone().getNom(),
                p.getDescription(), p.getEcheance(), p.getStatut(), ouvert && p.getEcheance().isBefore(aujourdhui),
                p.getNote(), ((u.getPrenom() == null ? "" : u.getPrenom()) + " " + (u.getNom() == null ? "" : u.getNom())).trim(),
                p.getCreatedAt(), p.getTermineLe());
    }
}
