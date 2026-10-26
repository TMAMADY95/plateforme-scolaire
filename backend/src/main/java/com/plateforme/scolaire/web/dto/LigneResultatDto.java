package com.plateforme.scolaire.web.dto;

import java.math.BigDecimal;
import java.util.Map;

/** Moyennes sur 20. moyenneGenerale et rang sont null tant que l'élève n'a aucune note. */
public record LigneResultatDto(Long inscriptionId, EleveDto eleve, Map<Long, BigDecimal> moyennesParMatiere,
                               BigDecimal moyenneGenerale, Integer rang) {
}
