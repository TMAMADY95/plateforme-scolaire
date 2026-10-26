package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Si l'adresse e-mail correspond déjà à un compte parent, l'élève lui est simplement rattaché
 * (nom, prénom et mot de passe sont alors ignorés). Sinon un compte est créé : le mot de passe initial est requis.
 */
public record ParentRequest(@NotBlank @Email @Size(max = 255) String email,
                            @NotBlank @Size(max = 100) String nom,
                            @NotBlank @Size(max = 100) String prenom,
                            @Size(max = 30) String telephone,
                            @NotBlank @Size(max = 50) String relation,
                            Boolean responsableLegal,
                            @Size(min = 10, max = 100) String motDePasseInitial) {
}
