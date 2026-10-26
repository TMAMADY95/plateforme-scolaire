package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ClasseRequest(@NotNull Long anneeScolaireId,
                            @NotNull Long niveauId,
                            @NotBlank @Size(max = 50) String nom,
                            @Positive Integer capacite) {
}
