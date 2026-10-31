package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.Min;

/** Nombre de postes d'enseignants prévus ; null pour effacer. */
public record PostesRequest(@Min(0) Integer postes) {
}
