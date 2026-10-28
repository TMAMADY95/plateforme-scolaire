package com.plateforme.scolaire.web.dto;

/** Chiffres clés nationaux pour une année scolaire. */
public record SyntheseNationaleDto(Long anneeScolaireId, long etablissements, long actifs, long suspendus,
                                   long fermes, long classes, long elevesInscrits) {
}
