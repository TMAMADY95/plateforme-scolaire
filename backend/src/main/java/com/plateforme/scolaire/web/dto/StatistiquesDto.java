package com.plateforme.scolaire.web.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Chiffres d'un ensemble d'établissements (un seul, une région ou le pays). [personnelDirection] : direction et
 * personnel administratif. [tauxPresence] : part des présences (présents et retards) sur les derniers
 * {@code joursPresence} jours, null quand aucune présence n'a été saisie. [effectifsParCycle] suit le cycle des
 * niveaux ; les élèves d'un niveau sans cycle sont regroupés sous « Cycle non défini ».
 */
public record StatistiquesDto(Long anneeScolaireId, long etablissements, long enseignants, long personnelDirection,
                              long classes, long elevesInscrits, BigDecimal tauxPresence, int joursPresence,
                              List<Repartition> repartitionParType, List<Repartition> repartitionParSecteur,
                              List<Repartition> effectifsParCycle, List<Repartition> effectifsParNiveau,
                              List<Repartition> effectifsParSexe, List<Repartition> effectifsParZone,
                              List<PointMois> inscriptionsParMois) {

    /** Un libellé et sa valeur (nombre d'établissements, d'élèves…). */
    public record Repartition(String libelle, long valeur) {
    }

    /** Nombre de demandes d'inscription d'un mois ("2026-09"). */
    public record PointMois(String mois, long nombre) {
    }
}
