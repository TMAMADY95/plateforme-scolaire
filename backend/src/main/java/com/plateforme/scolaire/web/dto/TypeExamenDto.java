package com.plateforme.scolaire.web.dto;

import com.plateforme.scolaire.domain.TypeExamen;

public record TypeExamenDto(Long id, String code, String libelle, String niveauOrganisation, Long niveauId,
                            String niveau, Long cycleId, String cycle) {

    public static TypeExamenDto de(TypeExamen t) {
        return new TypeExamenDto(t.getId(), t.getCode(), t.getLibelle(), t.getNiveauOrganisation(),
                t.getNiveau() == null ? null : t.getNiveau().getId(), t.getNiveau() == null ? null : t.getNiveau().getLibelle(),
                t.getCycle() == null ? null : t.getCycle().getId(), t.getCycle() == null ? null : t.getCycle().getLibelle());
    }
}
