package com.plateforme.scolaire.web.dto;

import java.time.Instant;

/** Une réclamation, avec l'indication de retard sur l'échéance de traitement. */
public record ReclamationExamenDto(Long id, Long candidatId, String numero, String candidat, String objet, String description, String statut,
        Instant deposeeLe, Instant echeanceLe, Instant traiteeLe, String decision, boolean enRetard) {
}
