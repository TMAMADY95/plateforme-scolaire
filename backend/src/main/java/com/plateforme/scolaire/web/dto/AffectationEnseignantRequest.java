package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.NotNull;

public record AffectationEnseignantRequest(@NotNull Long enseignantId, @NotNull Long matiereId) {
}
