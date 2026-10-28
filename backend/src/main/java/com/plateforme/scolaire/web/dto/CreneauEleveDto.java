package com.plateforme.scolaire.web.dto;

import com.plateforme.scolaire.domain.Creneau;
import java.time.LocalTime;

/** Un créneau vu par l'élève ou le parent : matière, enseignant, salle, jour et horaire. */
public record CreneauEleveDto(String matiereLibelle, String enseignantNom, String salleNom,
                              Short jourSemaine, LocalTime heureDebut, LocalTime heureFin) {

    public static CreneauEleveDto de(Creneau c) {
        String enseignantNom = c.getEnseignant() == null ? null
                : c.getEnseignant().getUtilisateur().getPrenom() + " "
                        + c.getEnseignant().getUtilisateur().getNom();
        return new CreneauEleveDto(
                c.getMatiere().getLibelle(), enseignantNom,
                c.getSalle() == null ? null : c.getSalle().getNom(),
                c.getJourSemaine(), c.getHeureDebut(), c.getHeureFin());
    }
}
