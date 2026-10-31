package com.plateforme.scolaire.web.dto;

/** Réponse (202) quand un code envoyé par e-mail doit être saisi pour terminer la connexion. */
public record VerificationRequiseResponse(boolean verificationRequise) {
}
