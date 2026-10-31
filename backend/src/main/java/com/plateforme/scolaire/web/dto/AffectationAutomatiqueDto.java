package com.plateforme.scolaire.web.dto;

import java.util.List;

/** Résultat de l'affectation automatique : candidats placés, et établissements restés sans centre dans leur commune. */
public record AffectationAutomatiqueDto(int candidatsAffectes, int etablissementsAffectes, List<String> sansCentre) {
}
