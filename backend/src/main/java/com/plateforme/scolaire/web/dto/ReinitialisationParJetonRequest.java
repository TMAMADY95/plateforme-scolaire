package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Nouveau mot de passe choisi avec le lien reçu par e-mail. */
public record ReinitialisationParJetonRequest(@NotBlank @Size(max = 200) String jeton,
                                              @NotBlank @Size(min = 10, max = 100) String nouveauMotDePasse) {
}
