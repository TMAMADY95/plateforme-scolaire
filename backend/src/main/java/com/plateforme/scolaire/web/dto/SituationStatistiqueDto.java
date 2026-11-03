package com.plateforme.scolaire.web.dto;

import java.time.Instant;
import java.util.List;

/** Une situation statistique figée, avec l'écart d'inscrits par rapport aux chiffres du moment. */
public record SituationStatistiqueDto(Long id, Long anneeScolaireId, String statut, Instant etablieLe, String etabliePar, String commentaire,
        int inscrits, int filles, int libres, int presents, int absents, int elimines, int admis, int centres, int salles, Double tauxReussite,
        int ecartInscritsActuel, List<Niveau> niveaux) {

    public record Niveau(String niveau, int inscrits, int filles, int presents, int admis, Double tauxReussite) {
    }
}
