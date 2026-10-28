package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

/**
 * Notification envoyée par le prestataire de paiement. Elle est signée (HMAC-SHA256) avec un secret partagé :
 * signature = HMAC(reference|statut|montant à 2 décimales|referenceExterne).
 */
public record NotificationPaiement(@NotBlank String reference,
                                   @NotBlank @Pattern(regexp = "SUCCES|ECHEC") String statut,
                                   @NotNull BigDecimal montant,
                                   String referenceExterne,
                                   @NotBlank String signature) {
}
