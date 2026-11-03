package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Pièce d'état civil : EXTRAIT, JUGEMENT_SUPPLETIF ou AUCUN (numéro obligatoire sauf pour AUCUN). */
public record EtatCivilRequest(
        @NotBlank @Pattern(regexp = "EXTRAIT|JUGEMENT_SUPPLETIF|AUCUN") String typeActeNaissance,
        @Size(max = 60) String numeroActeNaissance) {
}
