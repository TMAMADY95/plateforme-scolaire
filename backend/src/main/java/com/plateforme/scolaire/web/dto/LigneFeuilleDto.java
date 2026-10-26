package com.plateforme.scolaire.web.dto;

/** statut est null tant que l'appel n'a pas été fait pour cet élève. */
public record LigneFeuilleDto(Long inscriptionId, EleveDto eleve, StatutPresence statut, String motif,
                              boolean justifie) {
}
