package com.plateforme.scolaire.web.dto;

import java.time.Instant;

/** État de la liste des candidats d'un établissement dans le circuit de validation. */
public record ListeCandidatsDto(String statut, int candidats, int dossiersComplets, Instant transmiseLe, Instant valideeRegionLe,
                                Instant valideeLe, String motifRejet) {
}
