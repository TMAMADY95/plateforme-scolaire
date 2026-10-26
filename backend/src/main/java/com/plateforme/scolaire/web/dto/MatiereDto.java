package com.plateforme.scolaire.web.dto;

import com.plateforme.scolaire.domain.Matiere;

public record MatiereDto(Long id, String code, String libelle) {

    public static MatiereDto de(Matiere m) {
        return new MatiereDto(m.getId(), m.getCode(), m.getLibelle());
    }
}
