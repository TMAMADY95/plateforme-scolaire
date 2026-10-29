package com.plateforme.scolaire.web.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Taux de réussite d'un ensemble d'établissements (le pays ou une région) pour une période : part des élèves notés
 * dont la moyenne générale atteint le seuil. [taux] est null quand aucun élève n'a de note.
 */
public record ReussiteReseauDto(Long anneeScolaireId, Long periodeId, BigDecimal seuil, long evalues, long admis,
                                BigDecimal taux, List<Groupe> parNiveau, List<Groupe> parZone,
                                List<Groupe> etablissements) {

    /** Un niveau, une zone ou un établissement. */
    public record Groupe(String libelle, long evalues, long admis, BigDecimal taux) {
    }
}
