package com.plateforme.scolaire.web.dto;

import com.plateforme.scolaire.domain.Periode;
import java.time.LocalDate;

public record PeriodeDto(Long id, Long anneeScolaireId, String libelle, int ordre,
                         LocalDate dateDebut, LocalDate dateFin) {

    public static PeriodeDto de(Periode p) {
        return new PeriodeDto(p.getId(), p.getAnneeScolaire().getId(), p.getLibelle(), p.getOrdre(),
                p.getDateDebut(), p.getDateFin());
    }
}
