package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Création ou modification d'un établissement par le ministère. Au moins un cycle est obligatoire. */
public record EtablissementRequest(@NotBlank @Size(max = 50) String codeOfficiel,
                                   @NotBlank @Size(max = 200) String nom,
                                   Long zoneId,
                                   @NotBlank @Pattern(regexp = "PUBLIC|PRIVE", message = "Secteur : PUBLIC ou PRIVE") String secteur,
                                   @NotEmpty List<Long> cycleIds,
                                   @Size(max = 300) String adresse,
                                   @Positive Integer capacite,
                                   Long typeEtablissementId) {

    public EtablissementRequest(String codeOfficiel, String nom, Long zoneId, String secteur, List<Long> cycleIds,
                                String adresse, Integer capacite) {
        this(codeOfficiel, nom, zoneId, secteur, cycleIds, adresse, capacite, null);
    }
}
