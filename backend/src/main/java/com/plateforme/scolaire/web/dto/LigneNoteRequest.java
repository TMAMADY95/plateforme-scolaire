package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Trois cas : une valeur (note), absent = true (élève absent, pas de valeur),
 * ou ni l'un ni l'autre (efface la note saisie). "absent" peut être omis.
 */
public record LigneNoteRequest(@NotNull Long inscriptionId,
                               @DecimalMin("0") @Digits(integer = 3, fraction = 2) BigDecimal valeur,
                               Boolean absent) {

    /** Jackson 3 refuse un champ manquant pour un boolean primitif : on utilise un Boolean, null = faux. */
    public boolean estAbsent() {
        return Boolean.TRUE.equals(absent);
    }
}
