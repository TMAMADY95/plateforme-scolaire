package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/** Création ou mise à jour d'une commission : sans épreuve, c'est la commission générale de l'examen. */
public record CommissionCorrectionRequest(Long epreuveId, @Size(max = 150) String lieu, @Min(0) int nbCopies, @Min(0) int nbCopiesCorrigees) {
}
