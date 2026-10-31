package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

public record SortieRequest(@NotNull LocalDate dateSortie, @NotNull LocalTime heureSortie,
                            @NotBlank @Size(max = 500) String motif, @Size(max = 150) String accompagnateur) {
}
