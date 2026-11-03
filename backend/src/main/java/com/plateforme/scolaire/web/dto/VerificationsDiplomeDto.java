package com.plateforme.scolaire.web.dto;

import java.time.Instant;
import java.util.List;

/** Synthèse des vérifications publiques de diplômes ; les codes inconnus saisis plusieurs fois sont des tentatives suspectes. */
public record VerificationsDiplomeDto(long total, long authentiques, long revoques, long inconnus, long derniers30Jours, Double tauxInconnus,
        int seuilSuspect, List<Suspect> suspects) {

    public record Suspect(String code, long tentatives, Instant derniere) {
    }
}
