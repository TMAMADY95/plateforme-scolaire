package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/** Rapport de surveillance d'une épreuve : horaires réels, effectifs, copies ramassées. */
public record RapportEpreuveRequest(Instant compositionDebut, Instant compositionFin, @NotNull @Min(0) Integer presents,
                                    @NotNull @Min(0) Integer absents, @NotNull @Min(0) Integer elimines,
                                    @NotNull @Min(0) Integer copiesRamassees, @Size(max = 2000) String observations,
                                    boolean cloturer) {
}
