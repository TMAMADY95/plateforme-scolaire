package com.plateforme.scolaire.web.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Statistiques pour les tableaux de bord : pour un établissement, une région ou le pays selon l'endroit où elles sont
 * demandées. [tauxPresence] est un pourcentage (présents et retards sur toutes les présences saisies depuis
 * {@code joursPresence} jours), null quand aucune présence n'a été saisie.
 */
public record StatistiquesDto(Long anneeScolaireId, long etablissements, long enseignants, long classes,
                              long elevesInscrits, BigDecimal tauxPresence, int joursPresence,
                              List<Repartition> repartitionParType, List<Repartition> effectifsParNiveau,
                              List<Repartition> effectifsParZone, List<PointMois> inscriptionsParMois) {

    /** Un libellé et sa valeur (nombre d'établissements, d'élèves…). */
    public record Repartition(String libelle, long valeur) {
    }

    /** Nombre de demandes d'inscription d'un mois ("2026-09"). */
    public record PointMois(String mois, long nombre) {
    }
}
