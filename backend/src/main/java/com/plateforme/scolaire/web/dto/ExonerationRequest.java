package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** Exonération (bourse, remise, aide sociale) accordée sur une facture. */
public record ExonerationRequest(@NotNull @DecimalMin(value = "0.01") BigDecimal montant,
                                 @NotBlank @Size(max = 255) String motif,
                                 @Size(max = 30) String typeExoneration) {
}
