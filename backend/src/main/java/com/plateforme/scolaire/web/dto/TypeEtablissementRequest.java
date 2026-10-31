package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TypeEtablissementRequest(@NotBlank @Size(max = 30) String code, @NotBlank @Size(max = 150) String libelle) {
}
