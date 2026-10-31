package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Un centre d'examen : de préférence un établissement désigné ([etablissementId], le nom et la zone viennent de lui),
 * sinon un lieu saisi librement ([nom] obligatoire).
 */
public record CentreExamenRequest(@Size(max = 150) String nom, @Size(max = 255) String adresse, Long zoneId,
                                  LocalDate dateDebut, Long etablissementId) {

    public CentreExamenRequest(String nom, String adresse, Long zoneId, LocalDate dateDebut) {
        this(nom, adresse, zoneId, dateDebut, null);
    }
}
