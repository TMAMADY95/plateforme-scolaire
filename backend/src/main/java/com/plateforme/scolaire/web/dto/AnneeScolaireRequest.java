package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record AnneeScolaireRequest(@NotBlank @Size(max = 20) String libelle,
                                   @NotNull LocalDate dateDebut,
                                   @NotNull LocalDate dateFin) {
}
