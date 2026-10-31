package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record RendezVousRequest(@NotNull Long enseignantId, @NotNull LocalDateTime dateHeure,
                                @NotBlank @Size(max = 500) String motif) {
}
