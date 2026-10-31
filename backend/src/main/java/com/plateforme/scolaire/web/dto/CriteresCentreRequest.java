package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CriteresCentreRequest(
        @NotBlank String secteurAdmis,
        @NotNull Boolean autorisationRequise,
        @NotNull @Min(0) Integer sallesMinimum,
        @NotNull @Min(0) Integer capaciteMinimum) {
}
