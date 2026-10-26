package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/** coefficient vaut 1 et bareme vaut 20 par défaut. */
public record EvaluationRequest(@NotNull Long matiereId,
                                @NotNull Long periodeId,
                                @NotBlank @Size(max = 200) String titre,
                                @NotNull LocalDate dateEvaluation,
                                @DecimalMin("0.01") @Digits(integer = 2, fraction = 2) BigDecimal coefficient,
                                @DecimalMin("0.01") @Digits(integer = 3, fraction = 2) BigDecimal bareme) {
}
