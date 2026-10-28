package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

/**
 * Création ou modification d'un créneau. L'enseignant et la salle sont optionnels ;
 * le contrôle des conflits d'horaire est fait par le service.
 */
public record CreneauRequest(@NotNull Long matiereId,
                             Long enseignantId,
                             Long salleId,
                             @NotNull @Min(1) @Max(7) Short jourSemaine,
                             @NotNull LocalTime heureDebut,
                             @NotNull LocalTime heureFin) {
}
