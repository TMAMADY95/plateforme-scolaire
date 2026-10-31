package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.Size;

public record JuryMembreRequest(@Size(max = 150) String nom, boolean present) {
}
