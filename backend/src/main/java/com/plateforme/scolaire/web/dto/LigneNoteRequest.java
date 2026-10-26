package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Trois cas : une valeur (note), absent = true (élève absent, pas de valeur),
 * ou ni l'un ni l'autre (efface la note saisie).
 */
public record LigneNoteRequest(@NotNull Long inscriptionId,
                               @DecimalMin("0") @Digits(integer = 3, fraction = 2) BigDecimal valeur,
                               boolean absent) {
}
