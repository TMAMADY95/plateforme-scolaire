package com.plateforme.scolaire.web.dto;

import java.util.List;

/** Concordance entre les rapports de surveillance d'un centre et les statuts des candidats (présent, absent, éliminé). */
public record ConcordanceRapportsDto(Long examenId, Long centreId, int candidats, int presents, int absents, int elimines,
        boolean statutsComplets, List<Ecart> ecarts) {

    public record Ecart(String code, String gravite, Long epreuveId, String epreuve, String message) {}
}
