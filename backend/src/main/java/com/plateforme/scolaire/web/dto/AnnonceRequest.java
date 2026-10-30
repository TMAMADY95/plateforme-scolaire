package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnnonceRequest(@NotBlank @Size(max = 200) String titre, @NotBlank @Size(max = 4000) String contenu) {
}
