package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.NotBlank;

/** [type] : TRIMESTRE (3 périodes) ou SEMESTRE (2 périodes), de durées égales sur l'année scolaire. */
public record GenerationPeriodesRequest(@NotBlank String type) {
}
