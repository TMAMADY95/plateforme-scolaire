package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.Classe;
import com.plateforme.scolaire.domain.ProgrammeMatiere;
import com.plateforme.scolaire.repository.ProgrammeMatiereRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Programme appliqué à une classe : pour chaque matière, la ligne la plus précise parmi celles du niveau.
 * Une ligne propre au type d'établissement l'emporte sur une ligne commune, une ligne propre à la série l'emporte sur une ligne commune
 * à toutes les séries. Un type peut aussi ajouter une matière (arabe en franco-arabe).
 */
public final class ProgrammeApplicable {

    private ProgrammeApplicable() {
    }

    /** Pur : 0 (commune) à 3 (propre au type et à la série). */
    static int specificite(ProgrammeMatiere p) {
        return (p.getTypeEtablissement() != null ? 2 : 0) + (p.getSerie() != null ? 1 : 0);
    }

    /** Pur : une ligne par matière, la plus précise ; l'ordre de première apparition est conservé. */
    public static List<ProgrammeMatiere> retenir(List<ProgrammeMatiere> lignes) {
        Map<Long, ProgrammeMatiere> retenues = new LinkedHashMap<>();
        for (ProgrammeMatiere p : lignes) {
            ProgrammeMatiere deja = retenues.get(p.getMatiere().getId());
            if (deja == null || specificite(p) > specificite(deja)) {
                retenues.put(p.getMatiere().getId(), p);
            }
        }
        return List.copyOf(retenues.values());
    }

    /** Programme d'une classe selon son année, son niveau, sa série et le type de son établissement. */
    public static List<ProgrammeMatiere> pourClasse(ProgrammeMatiereRepository programmes, Classe classe) {
        Long type = classe.getEtablissement().getTypeEtablissement() == null ? null : classe.getEtablissement().getTypeEtablissement().getId();
        return retenir(programmes.pourClasse(classe.getAnneeScolaire().getId(), classe.getNiveau().getId(), classe.getSerie(), type));
    }
}
