package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CahierTexteRequest(@NotNull Long matiereId, @NotNull LocalDate dateSeance,
                                 @NotBlank @Size(max = 4000) String contenu) {
}
