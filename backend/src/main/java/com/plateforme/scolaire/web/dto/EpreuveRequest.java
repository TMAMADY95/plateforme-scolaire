package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

/** Une épreuve : une matière du référentiel ou, à défaut, un libellé ; coefficient 1 s'il n'est pas précisé. */
public record EpreuveRequest(Long matiereId, @Size(max = 150) String libelle, @NotNull LocalDate date,
                             @NotNull LocalTime heureDebut, @NotNull LocalTime heureFin, @Min(1) Integer coefficient) {

    public EpreuveRequest(Long matiereId, String libelle, LocalDate date, LocalTime heureDebut, LocalTime heureFin) {
        this(matiereId, libelle, date, heureDebut, heureFin, null);
    }
}
