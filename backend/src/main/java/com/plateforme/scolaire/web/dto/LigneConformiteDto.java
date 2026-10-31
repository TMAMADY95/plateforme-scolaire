package com.plateforme.scolaire.web.dto;

import java.time.LocalDate;
import java.util.List;

/** La situation d'un établissement : autorisation, dernière inspection, sécurité, données, urgence. */
public record LigneConformiteDto(
        Long id, String codeOfficiel, String nom, String secteur, List<CycleDto> cycles, String statut,
        Long zoneId, String zone,
        String autorisation, String reference, String motifStatut,
        LocalDate derniereInspection, String resultatInspection, boolean inspecte, boolean nonConforme,
        long signalementsOuverts, long signalementsGraves,
        LocalDate derniereDonnee, boolean sansDonnees,
        boolean urgent, int score, List<String> motifsUrgence) {
}
