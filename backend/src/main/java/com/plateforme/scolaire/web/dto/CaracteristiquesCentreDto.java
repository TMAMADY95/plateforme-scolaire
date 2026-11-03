package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Caractéristiques d'un centre d'examen (lecture et saisie). Les états valent BON, MOYEN ou MAUVAIS, ou null si non évalués. */
public record CaracteristiquesCentreDto(@Min(0) int sallesReserve,
        @Pattern(regexp = "BON|MOYEN|MAUVAIS") String etatTablesBancs, @Pattern(regexp = "BON|MOYEN|MAUVAIS") String etatToilettes,
        @Pattern(regexp = "BON|MOYEN|MAUVAIS") String etatEclairage, @Pattern(regexp = "BON|MOYEN|MAUVAIS") String etatVentilation,
        Boolean accessible, @Min(0) Integer distanceMaxKm, @Min(0) Integer tempsMaxMin, boolean risqueInondation, boolean risqueEnclavement,
        boolean risqueInsecurite, @Size(max = 500) String observation) {
}
