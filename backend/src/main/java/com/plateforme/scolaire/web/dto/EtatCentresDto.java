package com.plateforme.scolaire.web.dto;

import java.util.List;

/** État des centres d'examen d'une année : salles de réserve, installations, accessibilité, risques, et candidats en situation de handicap. */
public record EtatCentresDto(Long anneeScolaireId, Resume resume, List<LigneCentre> centres, List<Handicap> handicaps) {

    public record Resume(int centres, int renseignes, int sallesReserve, int centresInstallationsMauvaises, int centresInaccessibles, int centresARisque,
            int risqueInondation, int risqueEnclavement, int risqueInsecurite, Double moyenneCandidatsParCentre, int candidatsHandicapes) {
    }

    public record LigneCentre(Long examenId, Long centreId, String centre, String examen, String zone, int candidats, boolean renseigne, int sallesReserve,
            String etatTablesBancs, String etatToilettes, String etatEclairage, String etatVentilation, Boolean accessible, Integer distanceMaxKm,
            Integer tempsMaxMin, boolean risqueInondation, boolean risqueEnclavement, boolean risqueInsecurite, String observation) {
    }

    public record Handicap(String niveau, String type, int nombre) {
    }
}
