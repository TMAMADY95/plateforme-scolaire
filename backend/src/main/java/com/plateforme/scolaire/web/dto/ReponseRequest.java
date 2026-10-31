package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.Size;

/** Réponse à une demande : accepter (confirmer) ou refuser, avec un message facultatif. */
public record ReponseRequest(boolean accepter, @Size(max = 500) String reponse) {
}
