package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Annulation d'une rectification de note : motif obligatoire. */
public record AnnulationRectificationRequest(@NotBlank @Size(max = 500) String motif) {
}
