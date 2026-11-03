package com.plateforme.scolaire.web.dto;

import java.util.List;

/** Profil, participation et résultats des candidats aux examens d'une année (DGECS et ministère). */
public record StatistiquesCandidatsDto(Long anneeScolaireId, Long anneePrecedenteId, Ligne total, List<Ligne> parNiveau, List<Ligne> parZone,
        List<Ligne> parRegion, List<Ligne> parPrefecture, List<Mention> mentions) {

    /**
     * Une ligne d'agrégat : effectifs, participation, résultats et, pour les niveaux, la comparaison avec l'année précédente.
     * Les taux sont en pourcentage au dixième ; null quand le dénominateur est nul.
     */
    public record Ligne(String libelle, String detail, int inscrits, int filles, int garcons, int libres, int scolaires, int presents, int absents,
            int elimines, int admis, int presentsFilles, int admisFilles, Double pctFilles, Double pctLibres, Double tauxPresence, Double tauxAbsence,
            Double tauxReussite, Double tauxReussiteInscrits, Double tauxReussiteFilles, Double tauxReussiteGarcons, Integer inscritsPrecedent,
            Double tauxReussitePrecedent, Double evolutionInscrits) {
    }

    public record Mention(String mention, int nombre, Double pourcentage) {
    }
}
