package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Dépôt d'une réclamation : le candidat est désigné par son numéro de table dans l'examen. */
public record ReclamationExamenRequest(@NotBlank @Size(max = 30) String numeroCandidat, @NotBlank @Size(max = 20) String objet,
        @NotBlank @Size(max = 2000) String description) {
}
