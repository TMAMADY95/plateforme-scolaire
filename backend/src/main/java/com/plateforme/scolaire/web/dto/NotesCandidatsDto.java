package com.plateforme.scolaire.web.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Candidats d'un établissement à un examen avec leurs notes : tableau de saisie des résultats individuels. */
public record NotesCandidatsDto(Long examenId, String statutResultats, List<Epreuve> epreuves, List<Candidat> candidats) {

    public record Epreuve(Long id, String nom, int coefficient) {
    }

    public record Candidat(Long id, String nom, String sexe, String numeroPv, String statut, Map<Long, BigDecimal> notes,
                           BigDecimal moyenne, String decision) {
    }
}
