package com.plateforme.scolaire.web.dto;

import com.plateforme.scolaire.domain.TypeEtablissement;

public record TypeEtablissementDto(Long id, String code, String libelle) {

    public static TypeEtablissementDto de(TypeEtablissement t) {
        return new TypeEtablissementDto(t.getId(), t.getCode(), t.getLibelle());
    }
}
