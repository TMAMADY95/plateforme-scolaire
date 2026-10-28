package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record SalleRequest(@NotBlank @Size(max = 50) String nom,
                           @Positive Integer capacite,
                           @Size(max = 50) String typeSalle) {
}
