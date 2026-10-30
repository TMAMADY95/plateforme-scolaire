package com.plateforme.scolaire.web.dto;

import com.plateforme.scolaire.domain.CycleScolaire;

public record CycleDto(Long id, String code, String libelle, int ordre) {

    public static CycleDto de(CycleScolaire c) {
        return new CycleDto(c.getId(), c.getCode(), c.getLibelle(), c.getOrdre());
    }
}
