package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/**
 * Signalement d'un incident. {@code eliminer} (fraude avec un candidat) élimine le candidat : il est non admis à l'examen.
 * {@code survenuLe} vaut l'instant présent s'il n'est pas précisé.
 */
public record IncidentExamenRequest(@NotBlank String type, Long candidatId, Long epreuveId, @NotBlank @Size(max = 2000) String description,
                                    Instant survenuLe, @Size(max = 150) String signalePar, boolean eliminer) {
}
