package com.plateforme.scolaire.web.dto;

import com.plateforme.scolaire.domain.Classe;

public record ClasseDto(Long id, Long etablissementId, Long anneeScolaireId, Long niveauId,
                        String niveauLibelle, String nom, Integer capacite) {

    public static ClasseDto de(Classe c) {
        return new ClasseDto(
                c.getId(),
                c.getEtablissement().getId(),
                c.getAnneeScolaire().getId(),
                c.getNiveau().getId(),
                c.getNiveau().getLibelle(),
                c.getNom(),
                c.getCapacite());
    }
}
