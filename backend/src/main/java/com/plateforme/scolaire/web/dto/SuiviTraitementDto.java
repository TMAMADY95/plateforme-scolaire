package com.plateforme.scolaire.web.dto;

import java.util.List;

/**
 * Traitement des copies, notes et fiabilité du processus pour une année : correction, fraudes, doublons, notes par discipline
 * et distribution des moyennes. Les taux sont en pourcentage au dixième.
 */
public record SuiviTraitementDto(Long anneeScolaireId, Correction correction, Fraudes fraudes, Fiabilite fiabilite, List<Discipline> disciplines,
        List<Tranche> moyennes) {

    public record Effectif(String libelle, int nombre) {
    }

    public record Correction(int commissions, int cloturees, int copies, int corrigees, Double tauxCorrection, int presidents, int secretaires,
            int correcteurs, List<Effectif> correcteursParDiscipline) {
    }

    public record Fraudes(int incidents, int ouverts, int traites, int eliminesPourFraude, List<Effectif> parType) {
    }

    public record Fiabilite(int groupesIdentiteEnDouble, int inscriptionsMultiples, int notesManquantes) {
    }

    public record Discipline(String libelle, int notes, Double moyenne, Double minimum, Double maximum, int manquantes) {
    }

    public record Tranche(String libelle, int nombre, Double pourcentage) {
    }
}
