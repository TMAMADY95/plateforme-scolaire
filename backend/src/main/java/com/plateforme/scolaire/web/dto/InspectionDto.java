package com.plateforme.scolaire.web.dto;

import com.plateforme.scolaire.domain.Inspection;
import java.time.LocalDate;

public record InspectionDto(Long id, LocalDate dateInspection, String resultat, String observations, String inspecteur) {

    public static InspectionDto de(Inspection i) {
        String nom = ((i.getInspecteur().getPrenom() == null ? "" : i.getInspecteur().getPrenom()) + " "
                + (i.getInspecteur().getNom() == null ? "" : i.getInspecteur().getNom())).trim();
        return new InspectionDto(i.getId(), i.getDateInspection(), i.getResultat(), i.getObservations(), nom);
    }
}
