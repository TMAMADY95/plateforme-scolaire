package com.plateforme.scolaire.web.dto;

import java.util.List;

/** Personnel d'un centre et couverture des besoins : un président, un chef de centre, un secrétariat, 2 surveillants par salle. */
public record PersonnelCentreDto(Long centreId, String centre, int salles, List<Personne> personnel, List<Besoin> besoins,
                                 boolean complet) {

    public record Personne(Long id, String role, String nom, String telephone, Integer salle) {
    }

    public record Besoin(String libelle, int requis, int affectes) {

        public boolean satisfait() {
            return affectes >= requis;
        }
    }
}
