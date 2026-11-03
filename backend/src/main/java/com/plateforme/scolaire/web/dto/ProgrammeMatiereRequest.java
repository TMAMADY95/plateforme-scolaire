package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** Création (année, niveau, matière obligatoires) ou modification (seuls heures, coefficient et note comptent). */
public record ProgrammeMatiereRequest(Long anneeScolaireId, Long niveauId, @Size(max = 30) String serie, Long matiereId,
        @DecimalMin("0") @DecimalMax("99") BigDecimal heuresHebdo, @DecimalMin("0.01") @DecimalMax("99") BigDecimal coefficient,
        @Size(max = 300) String note) {}
