package com.plateforme.scolaire.web.dto;

import java.math.BigDecimal;
import java.util.List;

/** L'évolution d'année en année : effectifs et réussite aux examens nationaux. Les années les plus anciennes d'abord. */
public record EvolutionDto(List<PointAnnee> annees) {

    public record PointAnnee(Long anneeScolaireId, String libelle, long eleves, long classes,
                             long presentesExamens, long admisExamens, BigDecimal tauxExamens) {
    }
}
