package com.plateforme.scolaire.web.dto;

/**
 * Un élève qui peut être inscrit à l'examen : inscrit dans une classe concernée et pas déjà candidat.
 * {@code tauxAbsences} : pourcentage d'absences sur l'année (null si aucune présence saisie) ; {@code eligible} est faux
 * (avec un {@code motif}) quand le règlement général interdit de l'inscrire.
 */
public record ElevePossibleDto(Long eleveId, String nom, String matricule, String classe,
                               Integer tauxAbsences, boolean eligible, String motif) {
}
