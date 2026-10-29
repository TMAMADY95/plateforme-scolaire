package com.plateforme.scolaire.web.dto;

/** Une personne que l'on peut contacter à propos d'un élève. [role] : « Mathématiques », « Direction », « Père »… */
public record ContactDto(Long utilisateurId, String nom, String role) {
}
