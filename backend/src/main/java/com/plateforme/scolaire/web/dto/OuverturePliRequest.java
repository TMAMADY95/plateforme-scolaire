package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Ouverture du pli : le président de la commission de surveillance et deux témoins, trois personnes distinctes. */
public record OuverturePliRequest(@NotBlank @Size(max = 150) String ouvertPar, @NotBlank @Size(max = 150) String temoin1,
                                  @NotBlank @Size(max = 150) String temoin2) {
}
