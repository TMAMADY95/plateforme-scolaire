package com.plateforme.scolaire.web.dto;

import com.plateforme.scolaire.domain.CentreExamen;
import java.time.LocalDate;

public record CentreExamenDto(Long id, String nom, String adresse, Long zoneId, String zone, LocalDate dateDebut,
                              Long etablissementId) {

    public static CentreExamenDto de(CentreExamen c) {
        return new CentreExamenDto(c.getId(), c.getNom(), c.getAdresse(), c.getZone() == null ? null : c.getZone().getId(),
                c.getZone() == null ? null : c.getZone().getNom(), c.getDateDebut(),
                c.getEtablissement() == null ? null : c.getEtablissement().getId());
    }
}
