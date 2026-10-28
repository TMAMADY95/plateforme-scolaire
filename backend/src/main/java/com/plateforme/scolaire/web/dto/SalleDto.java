package com.plateforme.scolaire.web.dto;

import com.plateforme.scolaire.domain.Salle;

/** Une salle d'un établissement. */
public record SalleDto(Long id, Long etablissementId, String nom, Integer capacite, String typeSalle) {

    public static SalleDto de(Salle s) {
        return new SalleDto(s.getId(), s.getEtablissement().getId(), s.getNom(), s.getCapacite(),
                s.getTypeSalle());
    }
}
