package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.NotNull;

public record AffectationCentreRequest(@NotNull Long centreId) {
}
