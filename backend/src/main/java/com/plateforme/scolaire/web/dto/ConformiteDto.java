package com.plateforme.scolaire.web.dto;

import java.util.List;

/** Les indicateurs du tableau de bord « conformité et inspections », avec le détail par établissement. */
public record ConformiteDto(Indicateurs indicateurs, List<LigneConformiteDto> lignes) {

    public record Indicateurs(
            long total, long publics, long prives,
            long autorises, long nonAutorises, long autorisationEnAttente,
            long nonConformes, long inspectes, long avecProblemeSecurite,
            long sansDonnees, long fermes, long urgents) {
    }
}
