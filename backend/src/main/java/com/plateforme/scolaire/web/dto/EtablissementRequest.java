package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Création ou modification d'un établissement par le ministère. */
public record EtablissementRequest(@NotBlank @Size(max = 50) String codeOfficiel,
                                   @NotBlank @Size(max = 200) String nom,
                                   Long zoneId,
                                   @Size(max = 50) String typeEtablissement,
                                   @Size(max = 300) String adresse,
                                   @Positive Integer capacite) {
}
