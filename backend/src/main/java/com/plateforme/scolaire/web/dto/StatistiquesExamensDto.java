package com.plateforme.scolaire.web.dto;

import java.util.List;

/**
 * Statistiques des examens pour la DGECS et le ministère : salles et centres d'examen, résultats par niveau d'examen et par
 * type d'établissement. taux : pourcentage d'admis parmi les présents (null sans présent) ; remplissage : pourcentage de
 * places utilisées dans les salles d'examen (30 candidats par salle).
 */
public record StatistiquesExamensDto(Long anneeScolaireId, Salles salles, List<LigneCentre> centres, List<Niveau> niveaux,
        List<Resultats> types, List<LigneLycee> lycee) {

    /** Effectif des élèves inscrits au lycée (classes avec série) par niveau de classe et série. */
    public record LigneLycee(String niveau, String serie, int classes, int eleves) {}

    public record Salles(int centres, int sallesUtilisees, int candidatsAffectes, int candidatsSansCentre, int sallesDisponibles,
            int capaciteDisponible, Double remplissage) {}

    public record LigneCentre(Long centreId, String centre, String zone, String examen, int candidats, int sallesUtilisees,
            Integer sallesDisponibles, Integer capaciteDisponible, Double remplissage) {}

    /** Résultats d'un groupe (niveau d'examen, type d'établissement ou croisement des deux). */
    public record Resultats(String libelle, int etablissements, int inscrits, int presentes, int admis, int absents, int elimines,
            Double taux) {}

    /** Résultats d'une série du lycée (sciences mathématiques, sociales, expérimentales) ; parType : détail par type d'établissement. */
    public record Serie(String libelle, int inscrits, int presentes, int admis, Double taux, List<Serie> parType) {}

    /** Un niveau d'examen : total, détail par type d'établissement, puis par série (vide hors lycée). */
    public record Niveau(String niveau, int examens, Resultats total, List<Resultats> parType, List<Serie> parSerie) {}
}
