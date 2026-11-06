package com.plateforme.scolaire.web.dto;

import java.util.List;

/**
 * Passage à l'année suivante d'après les décisions du conseil de classe. anneeSuivanteId est null quand aucune année postérieure n'existe.
 * aReinscrire : passages (hors fin de parcours) et redoublements ; dejaInscrits : déjà inscrits l'année suivante.
 */
public record RentreeDto(Long etablissementId, Long anneeId, Long anneeSuivanteId, String anneeSuivante, Resume resume, List<Classe> classes) {

    public record Resume(int aReinscrire, int dejaInscrits, int finDeParcours, int exclus, int sansDecision) {
    }

    public record Classe(Long classeId, String classe, String niveau, int passages, int redoublements, int exclusions, int sansDecision, int finDeParcours) {
    }

    /** Résultat d'une préparation : inscriptions créées (en attente d'acceptation dans une classe) et apercu mis à jour. */
    public record Preparation(int crees, RentreeDto rentree) {
    }
}
