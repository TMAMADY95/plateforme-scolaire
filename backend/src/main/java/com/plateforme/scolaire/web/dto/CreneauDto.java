package com.plateforme.scolaire.web.dto;

import com.plateforme.scolaire.domain.Creneau;
import java.time.LocalTime;

/** Un créneau de l'emploi du temps d'une classe. */
public record CreneauDto(Long id, Long classeId, String classeNom, Long matiereId, String matiereLibelle,
                         Long enseignantId, String enseignantNom, Long salleId, String salleNom,
                         Short jourSemaine, LocalTime heureDebut, LocalTime heureFin) {

    public static CreneauDto de(Creneau c) {
        String enseignantNom = c.getEnseignant() == null ? null
                : c.getEnseignant().getUtilisateur().getPrenom() + " "
                        + c.getEnseignant().getUtilisateur().getNom();
        return new CreneauDto(
                c.getId(),
                c.getClasse().getId(), c.getClasse().getNom(),
                c.getMatiere().getId(), c.getMatiere().getLibelle(),
                c.getEnseignant() == null ? null : c.getEnseignant().getId(), enseignantNom,
                c.getSalle() == null ? null : c.getSalle().getId(),
                c.getSalle() == null ? null : c.getSalle().getNom(),
                c.getJourSemaine(), c.getHeureDebut(), c.getHeureFin());
    }
}
