package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Annulation d'un paiement hors ligne : le motif est obligatoire et conservé. */
public record AnnulationPaiementRequest(@NotBlank @Size(max = 255) String motif) {
}
