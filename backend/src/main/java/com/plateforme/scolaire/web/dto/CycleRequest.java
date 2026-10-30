package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CycleRequest(@NotBlank @Size(max = 30) String code,
                           @NotBlank @Size(max = 100) String libelle,
                           @NotNull @Min(1) Integer ordre) {
}
