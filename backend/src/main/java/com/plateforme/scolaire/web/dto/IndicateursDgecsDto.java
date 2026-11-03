package com.plateforme.scolaire.web.dto;

/**
 * Tableau de bord du directeur général : indicateurs de performance de l'organisation des examens d'une année.
 * Les taux sont en pourcentage au dixième ; null quand le dénominateur est nul.
 */
public record IndicateursDgecsDto(Long anneeScolaireId, int examens, int candidats, Double tauxListesValidees, Double tauxDossiersCompletes,
        Double tauxCentresPrets, Double tauxPostesPourvus, Double tauxReussite, Double incidentsPour10000, Integer delaiProclamationJours,
        Double tauxReclamationsDansLeDelai, Double tauxDiplomesDelivres, Double tauxExecutionBudget, Long coutParCandidat) {
}
