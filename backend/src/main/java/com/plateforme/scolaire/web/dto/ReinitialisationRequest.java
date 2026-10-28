package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Mot de passe temporaire fixé par l'administrateur : la personne devra le changer à sa prochaine connexion. */
public record ReinitialisationRequest(@NotBlank @Size(min = 10, max = 100) String motDePasseTemporaire) {
}
