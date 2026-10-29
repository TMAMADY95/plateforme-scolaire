package com.plateforme.scolaire.web.dto;

import java.math.BigDecimal;

/** coefficient : poids de la matière dans la moyenne générale de la classe (1 par défaut). */
public record LigneBulletinDto(Long matiereId, String matiere, BigDecimal moyenneEleve, BigDecimal moyenneClasse,
                               BigDecimal coefficient) {
}
