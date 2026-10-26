package com.plateforme.scolaire.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Soit le matricule d'un élève déjà connu du système (réinscription, transfert),
 * soit les données d'un nouvel élève : jamais les deux.
 * confirmerHomonyme = true autorise la création malgré un élève de même identité (jumeaux, homonymes).
 */
public record InscriptionRequest(@NotNull Long anneeScolaireId,
                                 @Size(max = 50) String matriculeEleve,
                                 @Valid EleveRequest nouvelEleve,
                                 Boolean confirmerHomonyme) {
}
