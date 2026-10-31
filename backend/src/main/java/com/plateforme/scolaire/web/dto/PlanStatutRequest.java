package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PlanStatutRequest(
        /** A_FAIRE, EN_COURS, TERMINE ou ANNULE */
        @NotBlank String statut,
        @Size(max = 1000) String note) {
}
