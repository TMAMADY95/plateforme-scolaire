package com.plateforme.scolaire.dto;

/** Anomalie détectée sur un candidat (candidatId nul pour une anomalie portant sur la liste entière). */
public record AnomalieExamenDto(String code, String gravite, String message, Long candidatId, String candidat, String numero) {}
