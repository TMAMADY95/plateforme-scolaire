package com.plateforme.scolaire.web.dto;

/**
 * Qualité des données des examens d'une année et suivi des réclamations. Les taux sont en pourcentage au dixième.
 */
public record QualiteDonneesDto(Long anneeScolaireId, int delaiReclamationJours, Dossiers dossiers, Reclamations reclamations,
        int resultatsRectifies) {

    public record Dossiers(int candidats, int sansCentre, int identiteIncomplete, int sansPhoto, int presentsSansMoyenne, int presentsSansDecision,
            int listesTotal, int listesValidees, Double tauxDossiersCompletes, Double tauxListesValidees) {
    }

    public record Reclamations(int total, int ouvertes, int acceptees, int rejetees, int enRetard, Double tauxTraiteesDansLeDelai,
            Double tauxAcceptation) {
    }
}
