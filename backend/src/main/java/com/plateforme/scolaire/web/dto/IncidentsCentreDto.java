package com.plateforme.scolaire.web.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Incidents d'un centre d'examen, avec les candidats et épreuves auxquels on peut les rattacher. */
public record IncidentsCentreDto(Long centreId, String centre, List<CandidatCite> candidats, List<EpreuveCitee> epreuves,
                                 List<Incident> incidents, Map<String, Long> parType) {

    public record CandidatCite(Long id, String nom, String numeroPv) {
    }

    public record EpreuveCitee(Long id, String nom) {
    }

    public record Incident(Long id, String type, String description, Instant survenuLe, String signalePar, String statut,
                           String decision, boolean elimine, Long candidatId, String candidat, String numeroPv,
                           Long epreuveId, String epreuve) {
    }
}
