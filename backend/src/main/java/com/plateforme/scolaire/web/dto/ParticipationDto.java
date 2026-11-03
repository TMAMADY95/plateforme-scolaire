package com.plateforme.scolaire.web.dto;

import java.util.List;

/**
 * Participation aux épreuves, d'après les rapports de surveillance des centres : candidats attendus, présents, absents, éliminés,
 * copies ramassées, rapports reçus et ponctualité (tolérance de 15 minutes). Les taux sont en pourcentage au dixième.
 */
public record ParticipationDto(Long anneeScolaireId, Resume resume, List<Epreuve> epreuves) {

    public record Resume(int epreuves, int rapportsAttendus, int rapportsRecus, int rapportsClos, int attendus, int presents, int absents, int elimines,
            Double tauxPresence, Double tauxAbsence, Double tauxDebutsALHeure, Double tauxFinsALHeure) {
    }

    public record Epreuve(Long epreuveId, String examen, String discipline, String date, String heureDebut, String heureFin, int centres, int rapportsRecus,
            int rapportsClos, int attendus, int presents, int absents, int elimines, int copiesRamassees, Double tauxPresence, Double tauxAbsence,
            int debutsALHeure, int debutsRenseignes, int finsALHeure, int finsRenseignees) {
    }

    public record Centre(Long centreId, String centre, String zone, boolean rapportRecu, boolean rapportClos, int attendus, Integer presents, Integer absents,
            Integer elimines, Integer copiesRamassees, Double tauxPresence, Double tauxAbsence, String debutReel, Boolean debutALHeure, Boolean finALHeure) {
    }
}
