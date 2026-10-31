package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.Min;
import java.util.List;

/**
 * Résultats d'un établissement. Seuls {@code presentes} et {@code admis} sont obligatoires ; les autres informations
 * (absents, éliminés, filles, séries du baccalauréat) enrichissent les statistiques quand elles sont connues.
 */
public record ResultatExamenRequest(@Min(0) int presentes, @Min(0) int admis, @Min(0) Integer absents,
                                    @Min(0) Integer elimines, @Min(0) Integer presentesFilles, @Min(0) Integer admisFilles,
                                    List<Serie> series) {

    public record Serie(String serie, @Min(0) int presentes, @Min(0) int admis) {
    }

    public ResultatExamenRequest(int presentes, int admis) {
        this(presentes, admis, null, null, null, null, null);
    }
}
