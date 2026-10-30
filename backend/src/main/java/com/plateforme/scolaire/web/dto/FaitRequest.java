package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.NotNull;

/** [eleveId] peut rester vide pour un élève qui coche son propre devoir ; un parent le précise. */
public record FaitRequest(Long eleveId, @NotNull Boolean fait) {
}
