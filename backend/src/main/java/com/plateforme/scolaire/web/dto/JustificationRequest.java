package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JustificationRequest(@NotBlank @Size(max = 500) String motif) {
}
