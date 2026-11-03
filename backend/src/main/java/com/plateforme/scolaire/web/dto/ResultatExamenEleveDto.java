package com.plateforme.scolaire.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Résultat d'un examen national pour un élève, visible seulement après la proclamation.
 * statut : PRESENT, ABSENT ou ELIMINE ; decision : ADMIS ou NON_ADMIS ; mention seulement pour un admis.
 */
public record ResultatExamenEleveDto(Long examenId, String examen, String anneeScolaire, int bareme, LocalDate dateProclamation,
        String numero, String statut, String decision, BigDecimal moyenne, String mention, List<NoteEpreuve> notes) {

    public record NoteEpreuve(String epreuve, int coefficient, BigDecimal note) {}
}
