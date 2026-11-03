package com.plateforme.scolaire.web.dto;

import java.time.LocalDate;
import java.util.List;

/** Calendrier prévisionnel, dates réelles et retards éventuels (CORRECTION, DELIBERATION, PROCLAMATION). */
public record CalendrierPrevisionnelDto(Long examenId, LocalDate finEpreuves, LocalDate correctionDebut,
        LocalDate correctionFin, LocalDate deliberation, LocalDate proclamation, String statutResultats,
        LocalDate deliberationReelle, LocalDate proclamationReelle, List<String> retards) {}
