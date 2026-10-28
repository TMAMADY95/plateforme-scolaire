package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Une zone (pays, région, département...) ; {@code parentId} à null pour la racine. */
public record ZoneRequest(Long parentId,
                          @NotBlank @Size(max = 150) String nom,
                          @NotBlank @Size(max = 50) String typeZone) {
}
