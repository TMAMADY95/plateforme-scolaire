package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record InspectionRequest(
        @NotNull LocalDate dateInspection,
        /** CONFORME ou NON_CONFORME */
        @NotBlank String resultat,
        @Size(max = 2000) String observations) {
}
