package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** Encaissement saisi par le personnel : espèces, virement ou chèque. */
public record PaiementRequest(@NotNull @DecimalMin(value = "0.01") BigDecimal montant,
                              @NotBlank String mode,
                              @Size(max = 100) String reference) {
}
