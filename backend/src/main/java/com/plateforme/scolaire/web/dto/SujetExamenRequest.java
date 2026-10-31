package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SujetExamenRequest(@NotBlank String variante, @NotBlank @Size(max = 200) String intitule, @Size(max = 300) String auteurs) {
}
