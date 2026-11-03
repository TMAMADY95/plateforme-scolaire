package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Prévision ou suivi d'une fourniture pour un centre ; la quantité livrée n'est connue qu'à la réception. */
public record LivraisonCentreRequest(@NotBlank @Size(max = 20) String article, @NotNull @Min(0) Integer quantitePrevue, @Min(0) Integer quantiteLivree,
        @NotBlank @Size(max = 10) String statut, @Size(max = 500) String observation) {
}
