package com.plateforme.scolaire.web.dto;

import java.time.LocalDate;

/** Période officielle de l'examen ; les deux dates peuvent être vides tant qu'elles ne sont pas publiées. */
public record PeriodeExamenRequest(LocalDate dateDebut, LocalDate dateFin) {
}
