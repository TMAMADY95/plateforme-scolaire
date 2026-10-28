package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Génération des factures pour une année scolaire.
 * Exactement l'un des deux : {@code classeId} pour une classe, ou {@code niveauId}
 * pour tous les élèves d'un niveau dans l'établissement.
 */
public record GenererFacturesRequest(@NotNull Long anneeScolaireId,
                                     Long classeId,
                                     Long niveauId) {
}
