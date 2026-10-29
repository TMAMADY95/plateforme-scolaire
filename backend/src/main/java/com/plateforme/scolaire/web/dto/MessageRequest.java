package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MessageRequest(@NotBlank @Size(max = 4000) String contenu) {
}
