package com.plateforme.scolaire.web.dto;

/** Un centre d'examen confié au chef de centre connecté. */
public record MonCentreDto(Long examenId, String examen, String statutResultats, Long centreId, String centre, String zone) {
}
