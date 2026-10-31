package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MembreCommissionRequest(@NotBlank String role, @NotBlank @Size(max = 150) String nom, @Size(max = 40) String telephone) {
}
