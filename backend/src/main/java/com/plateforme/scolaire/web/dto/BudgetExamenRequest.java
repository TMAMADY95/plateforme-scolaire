package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Montants d'une rubrique du budget d'un examen, en francs guinéens. */
public record BudgetExamenRequest(@NotBlank @Size(max = 20) String rubrique, @NotNull @Min(0) Long montantPrevu, @NotNull @Min(0) Long montantEngage,
        @NotNull @Min(0) Long montantPaye, @Size(max = 500) String observation) {
}
