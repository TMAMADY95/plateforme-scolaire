package com.plateforme.scolaire.web.dto;

import com.plateforme.scolaire.domain.CritereCentreExamen;

/** Conditions pour être centre d'examen : secteur admis (TOUS ou PUBLIC), autorisation, salles et places minimales. */
public record CriteresCentreDto(String secteurAdmis, boolean autorisationRequise, int sallesMinimum, int capaciteMinimum) {

    public static CriteresCentreDto de(CritereCentreExamen c) {
        return new CriteresCentreDto(c.getSecteurAdmis(), c.isAutorisationRequise(), c.getSallesMinimum(), c.getCapaciteMinimum());
    }
}
