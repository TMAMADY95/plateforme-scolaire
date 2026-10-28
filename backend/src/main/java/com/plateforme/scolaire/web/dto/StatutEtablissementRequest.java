package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** ACTIF, SUSPENDU ou FERME. */
public record StatutEtablissementRequest(
        @NotBlank @Pattern(regexp = "ACTIF|SUSPENDU|FERME", message = "ACTIF, SUSPENDU ou FERME attendu") String statut) {
}
