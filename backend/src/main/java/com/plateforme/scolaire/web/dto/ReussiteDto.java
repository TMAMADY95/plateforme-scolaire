package com.plateforme.scolaire.web.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Taux de réussite d'un établissement pour une période : part des élèves dont la moyenne générale atteint le seuil.
 * Seuls les élèves ayant au moins une note comptent (taux null quand il n'y en a aucun).
 */
public record ReussiteDto(Long etablissementId, Long anneeScolaireId, Long periodeId, BigDecimal seuil,
                          long evalues, long admis, BigDecimal taux, List<ReussiteClasseDto> classes) {

    public record ReussiteClasseDto(Long classeId, String classe, String niveau, long evalues, long admis,
                                    BigDecimal taux, BigDecimal moyenneClasse) {
    }
}
