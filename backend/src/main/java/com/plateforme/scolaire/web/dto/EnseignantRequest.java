package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EnseignantRequest(@NotBlank @Email @Size(max = 255) String email,
                                @NotBlank @Size(max = 100) String nom,
                                @NotBlank @Size(max = 100) String prenom,
                                @Size(max = 30) String telephone,
                                @Size(max = 50) String matricule,
                                @NotBlank @Size(min = 10, max = 100) String motDePasseInitial) {
}
