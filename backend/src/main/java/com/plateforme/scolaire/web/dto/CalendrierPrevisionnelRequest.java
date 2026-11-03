package com.plateforme.scolaire.web.dto;

import java.time.LocalDate;

/** Dates prévisionnelles ; chacune peut rester vide tant qu'elle n'est pas arrêtée. */
public record CalendrierPrevisionnelRequest(LocalDate correctionDebut, LocalDate correctionFin, LocalDate deliberation,
        LocalDate proclamation) {}
