package com.plateforme.scolaire.web.dto;

import java.util.List;

/**
 * Plan d'un centre d'examen : candidats répartis par salle (30 au plus) et par table. {@code repartie} est faux tant que
 * la répartition n'a pas été générée ; {@code surveillants} : deux par salle, selon le règlement général.
 */
public record RepartitionCentreDto(Long centreId, String centre, int candidats, int salles, int surveillants,
                                   Long capacite, boolean repartie, List<Salle> plan) {

    public record Salle(int numero, List<Place> places) {
    }

    public record Place(int table, String numeroPv, String nom, String matricule, String etablissement) {
    }
}
