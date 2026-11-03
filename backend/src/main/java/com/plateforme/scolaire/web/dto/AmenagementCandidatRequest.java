package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.Size;

/** Handicap du candidat et aménagement accordé ; un handicap vide retire la mention. */
public record AmenagementCandidatRequest(@Size(max = 15) String handicap, @Size(max = 200) String amenagement) {
}
