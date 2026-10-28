package com.plateforme.scolaire.web.dto;

import java.math.BigDecimal;

/** Synthèse financière d'un établissement pour une année scolaire. */
public record BilanFinancierDto(Long etablissementId, Long anneeScolaireId, String devise,
                                BigDecimal totalDu, BigDecimal totalExonere, BigDecimal totalPaye,
                                BigDecimal totalRestant,
                                long nombreFactures, long nombreImpayees, long nombrePayees,
                                long nombreEnRetard) {
}
