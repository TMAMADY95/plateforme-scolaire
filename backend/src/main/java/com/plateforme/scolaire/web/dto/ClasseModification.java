package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** L'année scolaire et l'établissement d'une classe ne se modifient pas. */
public record ClasseModification(@NotNull Long niveauId,
                                 @NotBlank @Size(max = 50) String nom,
                                 @Positive Integer capacite) {
}
