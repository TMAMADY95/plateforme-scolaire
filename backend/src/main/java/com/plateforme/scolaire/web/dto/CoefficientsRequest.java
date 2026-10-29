package com.plateforme.scolaire.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/** Les matières absentes de la liste ne sont pas modifiées. */
public record CoefficientsRequest(@NotEmpty @Size(max = 100) List<@Valid Ligne> lignes) {

    public record Ligne(@NotNull Long matiereId,
                        @NotNull @DecimalMin("0.01") @Digits(integer = 2, fraction = 2) BigDecimal coefficient) {
    }
}
