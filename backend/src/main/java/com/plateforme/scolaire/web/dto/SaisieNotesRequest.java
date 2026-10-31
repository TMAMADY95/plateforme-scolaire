package com.plateforme.scolaire.web.dto;

import java.math.BigDecimal;
import java.util.Map;

/** Statut d'un candidat (PRESENT, ABSENT, ELIMINE ou INSCRIT) et ses notes par épreuve. */
public record SaisieNotesRequest(String statut, Map<Long, BigDecimal> notes) {
}
