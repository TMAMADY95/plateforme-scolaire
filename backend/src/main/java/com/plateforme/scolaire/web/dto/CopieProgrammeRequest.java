package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.NotNull;

public record CopieProgrammeRequest(@NotNull Long deAnneeScolaireId, @NotNull Long versAnneeScolaireId) {}
