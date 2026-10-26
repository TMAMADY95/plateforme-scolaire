package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.NotNull;

/** classeId est obligatoire pour accepter une inscription. */
public record DecisionRequest(@NotNull DecisionInscription decision, Long classeId) {
}
