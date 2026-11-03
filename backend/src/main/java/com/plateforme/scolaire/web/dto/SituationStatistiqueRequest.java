package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Demande d'établissement d'une situation statistique : PROVISOIRE ou DEFINITIVE. */
public record SituationStatistiqueRequest(@NotBlank @Size(max = 10) String statut, @Size(max = 500) String commentaire) {
}
