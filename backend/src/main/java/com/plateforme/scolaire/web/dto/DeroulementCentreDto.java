package com.plateforme.scolaire.web.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** Déroulement des épreuves dans un centre : circuit des plis, ouverture et rapports de surveillance. */
public record DeroulementCentreDto(Long centreId, String centre, int candidats, List<Epreuve> epreuves) {

    /** {@code ecartCopies} : copies manquantes (négatif) ou en trop (positif) par rapport aux présents et éliminés. */
    public record Epreuve(Long epreuveId, String nom, LocalDate date, LocalTime heureDebut, LocalTime heureFin, int nbExemplaires,
                          String statutPli, String etapeSuivante, Instant conditionneLe, Instant expedieLe, Instant recuDpeLe,
                          Instant recuPresidentLe, Instant ouvertLe, String ouvertPar, String temoin1, String temoin2,
                          Instant compositionDebut, Instant compositionFin, Integer presents, Integer absents, Integer elimines,
                          Integer copiesRamassees, String observations, boolean rapportClos, Integer ecartCopies) {
    }
}
