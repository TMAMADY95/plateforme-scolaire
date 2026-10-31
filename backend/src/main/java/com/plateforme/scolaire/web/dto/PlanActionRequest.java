package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record PlanActionRequest(@NotBlank @Size(max = 2000) String description, @NotNull LocalDate echeance,
                                Long inspectionId) {
}
