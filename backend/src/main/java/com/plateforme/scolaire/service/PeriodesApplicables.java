package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.Periode;
import java.util.List;

/**
 * Quelles périodes s'appliquent à un établissement pour une année : les siennes s'il en a défini, sinon celles du
 * ministère. Les classes d'examen suivent la même règle que les autres.
 */
public final class PeriodesApplicables {

    private PeriodesApplicables() {
    }

    /** [toutes] : les périodes de l'année (nationales et d'établissements), triées par ordre. */
    public static List<Periode> pour(List<Periode> toutes, Long etablissementId) {
        if (etablissementId != null) {
            List<Periode> propres = toutes.stream()
                    .filter(p -> p.getEtablissement() != null && etablissementId.equals(p.getEtablissement().getId()))
                    .toList();
            if (!propres.isEmpty()) {
                return propres;
            }
        }
        return nationales(toutes);
    }

    public static List<Periode> nationales(List<Periode> toutes) {
        return toutes.stream().filter(p -> p.getEtablissement() == null).toList();
    }
}
