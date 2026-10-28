package com.plateforme.scolaire.web.dto;

import java.math.BigDecimal;

/** Résultat d'une génération de factures : ce qui a été créé, ce qui existait déjà. */
public record ResultatGenerationDto(int facturesCreees, int facturesIgnorees, int elevesTraites,
                                    BigDecimal montantTotalEmis) {
}
