package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Création ou modification d'un frais. niveauId à null = tous les niveaux. */
public record FraisRequest(@NotNull Long anneeScolaireId,
                           Long niveauId,
                           @NotBlank @Size(max = 150) String libelle,
                           @NotNull @DecimalMin(value = "0.00") BigDecimal montant,
                           @Size(max = 3) String devise,
                           LocalDate echeance,
                           Boolean obligatoire,
                           Boolean actif) {
}
