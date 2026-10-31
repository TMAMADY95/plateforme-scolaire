package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** [typePeriode] : TRIMESTRE, SEMESTRE ou AUTRE (absent = AUTRE). */
public record PeriodeEtablissementRequest(@NotBlank @Size(max = 50) String libelle, @NotNull @Min(1) Integer ordre,
                                          @NotNull LocalDate dateDebut, @NotNull LocalDate dateFin,
                                          @Size(max = 10) String typePeriode) {
}
