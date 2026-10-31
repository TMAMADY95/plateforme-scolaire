package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignalementRequest(
        /** INCENDIE, BATIMENT, EAU_HYGIENE, CLOTURE ou AUTRE */
        @NotBlank String categorie,
        /** FAIBLE, MOYENNE ou ELEVEE */
        @NotBlank String gravite,
        @NotBlank @Size(max = 2000) String description) {
}
