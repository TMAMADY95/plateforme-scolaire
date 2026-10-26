package com.plateforme.scolaire.web.dto;

import com.plateforme.scolaire.domain.AnneeScolaire;
import java.time.LocalDate;

public record AnneeScolaireDto(Long id, String libelle, LocalDate dateDebut, LocalDate dateFin) {

    public static AnneeScolaireDto de(AnneeScolaire a) {
        return new AnneeScolaireDto(a.getId(), a.getLibelle(), a.getDateDebut(), a.getDateFin());
    }
}
