package com.plateforme.scolaire.web.dto;

import com.plateforme.scolaire.domain.ZoneGeographique;

public record ZoneDto(Long id, Long parentId, String nom, String typeZone) {

    public static ZoneDto de(ZoneGeographique z) {
        return new ZoneDto(z.getId(), z.getParent() == null ? null : z.getParent().getId(),
                z.getNom(), z.getTypeZone());
    }
}
