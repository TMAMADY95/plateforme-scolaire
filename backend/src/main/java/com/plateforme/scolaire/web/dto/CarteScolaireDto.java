package com.plateforme.scolaire.web.dto;

import java.util.List;

/**
 * Besoins en salles et en enseignants calculés à partir d'une norme saisie par l'utilisateur (élèves par salle, élèves par enseignant).
 * manqueSalles est null tant que la fiche d'infrastructure de l'établissement n'est pas renseignée.
 */
public record CarteScolaireDto(Long anneeId, int normeElevesParSalle, int normeElevesParEnseignant, Total total, List<Zone> zones, List<Ligne> etablissements) {

    public record Ligne(Long etablissementId, String etablissement, String zone, int eleves, Integer salles, int sallesNecessaires, Integer manqueSalles,
            int enseignants, int enseignantsNecessaires, int manqueEnseignants) {
    }

    public record Zone(String zone, int etablissements, int eleves, int manqueSalles, int manqueEnseignants, int fichesManquantes) {
    }

    public record Total(int etablissements, int eleves, int manqueSalles, int manqueEnseignants, int fichesManquantes) {
    }
}
