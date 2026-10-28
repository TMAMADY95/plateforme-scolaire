package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;

/** Paiement en ligne : montant facultatif (par défaut, tout le reste à payer). */
public record InitierPaiementRequest(@DecimalMin(value = "0.01") BigDecimal montant) {
}
