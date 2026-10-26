package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangementMotDePasseRequest(@NotBlank String motDePasseActuel,
                                          @NotBlank @Size(min = 10, max = 100) String nouveauMotDePasse) {
}
