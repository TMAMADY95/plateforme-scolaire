package com.plateforme.scolaire.web.dto;

import java.math.BigDecimal;

/** Réponse à l'initiation : l'adresse où envoyer le payeur pour finir son paiement. */
public record PaiementInitieDto(String reference, String urlPaiement, BigDecimal montant, String devise) {
}
