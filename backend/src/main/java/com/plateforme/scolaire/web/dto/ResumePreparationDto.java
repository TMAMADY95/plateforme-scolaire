package com.plateforme.scolaire.web.dto;

/** État de préparation d'un centre, pour le suivi par les directions. */
public record ResumePreparationDto(Long centreId, String centre, String zone, int faits, int total, boolean pret) {
}
