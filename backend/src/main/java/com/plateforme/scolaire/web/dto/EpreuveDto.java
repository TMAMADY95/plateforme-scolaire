package com.plateforme.scolaire.web.dto;

import com.plateforme.scolaire.domain.EpreuveExamen;
import java.time.LocalDate;
import java.time.LocalTime;

public record EpreuveDto(Long id, Long examenId, Long matiereId, String matiere, LocalDate date, LocalTime heureDebut,
                         LocalTime heureFin, int coefficient) {

    public static EpreuveDto de(EpreuveExamen e) {
        return new EpreuveDto(e.getId(), e.getExamen().getId(), e.getMatiere() == null ? null : e.getMatiere().getId(),
                e.nom(), e.getDateEpreuve(), e.getHeureDebut(), e.getHeureFin(), e.getCoefficient());
    }
}
