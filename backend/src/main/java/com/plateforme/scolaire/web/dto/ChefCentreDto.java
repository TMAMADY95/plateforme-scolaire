package com.plateforme.scolaire.web.dto;

/** Un chef de centre, ou un centre confié à l'utilisateur connecté. */
public record ChefCentreDto(Long utilisateurId, String nom, String prenom, String email, boolean actif) {
}
