package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** [code] : code de vérification reçu par e-mail, seulement pour les comptes à double authentification. */
public record LoginRequest(@NotBlank @Email String email, @NotBlank String motDePasse, @Size(max = 10) String code) {

    public LoginRequest(String email, String motDePasse) {
        this(email, motDePasse, null);
    }
}
