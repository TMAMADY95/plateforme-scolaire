package com.plateforme.scolaire.web.dto;

import java.math.BigDecimal;

/** valeur est null et absent = false tant que l'élève n'a pas de note. */
public record LigneNoteDto(Long inscriptionId, EleveDto eleve, BigDecimal valeur, boolean absent) {
}
