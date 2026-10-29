package com.plateforme.scolaire.web.dto;

import java.math.BigDecimal;

/** [defini] est faux quand la matière utilise le coefficient par défaut (1). */
public record CoefficientMatiereDto(Long matiereId, String code, String libelle, BigDecimal coefficient,
                                    boolean defini) {
}
