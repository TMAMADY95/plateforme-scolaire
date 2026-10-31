package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.NotNull;

public record MutationRequest(@NotNull Long depuisEtablissementId, @NotNull Long versEtablissementId) {
}
