package com.plateforme.scolaire.web.dto;

import java.math.BigDecimal;

public record LigneBulletinDto(Long matiereId, String matiere, BigDecimal moyenneEleve, BigDecimal moyenneClasse) {
}
