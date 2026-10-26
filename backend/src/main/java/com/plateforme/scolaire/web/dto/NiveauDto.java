package com.plateforme.scolaire.web.dto;

import com.plateforme.scolaire.domain.Niveau;

public record NiveauDto(Long id, String code, String libelle, int ordre) {

    public static NiveauDto de(Niveau n) {
        return new NiveauDto(n.getId(), n.getCode(), n.getLibelle(), n.getOrdre());
    }
}
