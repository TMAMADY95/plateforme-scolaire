package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PreparationCentreRequest(@NotBlank String element, boolean fait, @Size(max = 500) String observation) {
}
