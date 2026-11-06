package com.plateforme.scolaire.web.dto;

import java.util.List;

/** Annuaire statistique d'une année scolaire sur le périmètre de l'utilisateur, avec les rubriques de l'annuaire officiel. */
public record AnnuaireDto(Long anneeId, String annee, Generalites generalites, List<Niveau> niveaux, List<Age> ages, List<Type> types, Enseignants enseignants) {

    /** elevesParClasse et elevesParEnseignant : une décimale ; null sans classe ou sans enseignant. */
    public record Generalites(int etablissements, int classes, int eleves, int filles, int redoublants, Double elevesParClasse, Double elevesParEnseignant) {
    }

    public record Niveau(Long niveauId, String niveau, int classes, int eleves, int filles, int redoublants, Double elevesParClasse) {
    }

    public record Age(int age, int garcons, int filles) {
    }

    public record Type(String type, int etablissements, int eleves) {
    }

    public record Enseignants(int total, int femmes, int profilsRenseignes, int formes, int nonTitulaires) {
    }
}
