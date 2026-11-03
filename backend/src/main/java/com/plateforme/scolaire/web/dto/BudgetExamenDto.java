package com.plateforme.scolaire.web.dto;

import java.util.List;

/** Budget d'un examen : lignes par rubrique, totaux, taux d'exécution et coûts unitaires (francs guinéens). */
public record BudgetExamenDto(Long examenId, int candidats, int centres, List<Ligne> lignes, Ligne total, Double tauxEngagement, Double tauxPaiement,
        Long coutParCandidat, Long coutParCentre) {

    /** Écart = engagé − prévu : positif en cas de dépassement. */
    public record Ligne(String rubrique, long prevu, long engage, long paye, long ecart, String observation) {
    }
}
