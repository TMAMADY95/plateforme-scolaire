package com.plateforme.scolaire.web.dto;

import com.plateforme.scolaire.domain.AutorisationSortie;
import java.time.LocalDate;
import java.time.LocalTime;

public record SortieDto(Long id, Long eleveId, String eleve, LocalDate dateSortie, LocalTime heureSortie, String motif,
                        String accompagnateur, String statut, String reponse) {

    public static SortieDto de(AutorisationSortie s) {
        return new SortieDto(s.getId(), s.getEleve().getId(), (s.getEleve().getPrenom() + " " + s.getEleve().getNom()).trim(),
                s.getDateSortie(), s.getHeureSortie(), s.getMotif(), s.getAccompagnateur(), s.getStatut(), s.getReponse());
    }
}
