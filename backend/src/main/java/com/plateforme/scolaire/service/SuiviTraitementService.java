package com.plateforme.scolaire.service;

import com.plateforme.scolaire.web.dto.SuiviTraitementDto;
import com.plateforme.scolaire.web.dto.SuiviTraitementDto.Discipline;
import com.plateforme.scolaire.web.dto.SuiviTraitementDto.Effectif;
import com.plateforme.scolaire.web.dto.SuiviTraitementDto.Tranche;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Traitement des copies et fiabilité du processus (DGECS, consultation ministère) : correction, fraudes, doublons, notes manquantes,
 * moyennes par discipline et distribution des moyennes. Les notes passent par des requêtes SQL agrégées : aucune note n'est chargée en mémoire.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SuiviTraitementService {

    static final List<String> TRANCHES = List.of("Moins de 25 % du barème", "25 % à moins de 50 %", "50 % à moins de 75 %", "75 % et plus");

    private final EntityManager em;

    private static int entier(Object o) {
        return o == null ? 0 : ((Number) o).intValue();
    }

    private static Double decimal(Object o) {
        return o == null ? null : BigDecimal.valueOf(((Number) o).doubleValue()).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    /** Pur : répartition des effectifs par tranche, avec le pourcentage de l'ensemble. */
    static List<Tranche> tranches(int[] effectifs) {
        int total = 0;
        for (int e : effectifs) {
            total += e;
        }
        List<Tranche> res = new ArrayList<>();
        for (int i = 0; i < TRANCHES.size(); i++) {
            res.add(new Tranche(TRANCHES.get(i), effectifs[i], StatistiquesCandidatsService.pct(effectifs[i], total)));
        }
        return res;
    }

    @SuppressWarnings("unchecked")
    public SuiviTraitementDto suivi(Long anneeId) {
        // ----- Correction -----
        Object[] c = (Object[]) em.createQuery("select count(c.id), coalesce(sum(case when c.cloturee = true then 1 else 0 end), 0), "
                + "coalesce(sum(c.nbCopies), 0), coalesce(sum(c.nbCopiesCorrigees), 0) from CommissionCorrection c where c.examen.anneeScolaire.id = :a")
                .setParameter("a", anneeId).getSingleResult();
        int presidents = 0;
        int secretaires = 0;
        int correcteurs = 0;
        List<Effectif> parDiscipline = new ArrayList<>();
        for (Object[] r : (List<Object[]>) em.createQuery("select m.role, coalesce(mt.libelle, e.libelle, 'Toutes disciplines'), count(m.id) "
                + "from MembreCommissionCorrection m join m.commission c left join c.epreuve e left join e.matiere mt "
                + "where c.examen.anneeScolaire.id = :a group by m.role, mt.libelle, e.libelle").setParameter("a", anneeId).getResultList()) {
            int n = entier(r[2]);
            switch ((String) r[0]) {
                case "PRESIDENT" -> presidents += n;
                case "SECRETAIRE" -> secretaires += n;
                default -> {
                    correcteurs += n;
                    parDiscipline.add(new Effectif((String) r[1], n));
                }
            }
        }
        parDiscipline.sort(java.util.Comparator.comparing(Effectif::libelle));
        int copies = entier(c[2]);
        int corrigees = entier(c[3]);
        var correction = new SuiviTraitementDto.Correction(entier(c[0]), entier(c[1]), copies, corrigees, StatistiquesCandidatsService.pct(corrigees, copies),
                presidents, secretaires, correcteurs, parDiscipline);

        // ----- Fraudes et incidents -----
        List<Effectif> parType = new ArrayList<>();
        int ouverts = 0;
        int traites = 0;
        int incidents = 0;
        for (Object[] r : (List<Object[]>) em.createQuery("select i.type, i.statut, count(i.id) from IncidentExamen i "
                + "where i.examen.anneeScolaire.id = :a group by i.type, i.statut").setParameter("a", anneeId).getResultList()) {
            int n = entier(r[2]);
            incidents += n;
            if ("OUVERT".equals(r[1])) {
                ouverts += n;
            } else {
                traites += n;
            }
            parType.add(new Effectif((String) r[0] + "|" + r[1], n));
        }
        List<Effectif> types = new ArrayList<>();
        for (Effectif e : parType) {
            String type = e.libelle().split("\\|")[0];
            var existant = types.stream().filter(x -> x.libelle().equals(type)).findFirst();
            if (existant.isPresent()) {
                types.set(types.indexOf(existant.get()), new Effectif(type, existant.get().nombre() + e.nombre()));
            } else {
                types.add(new Effectif(type, e.nombre()));
            }
        }
        int elimines = entier(em.createQuery("select count(i.id) from IncidentExamen i where i.examen.anneeScolaire.id = :a and i.elimine = true")
                .setParameter("a", anneeId).getSingleResult());
        var fraudes = new SuiviTraitementDto.Fraudes(incidents, ouverts, traites, elimines, types);

        // ----- Fiabilité -----
        int identites = ((List<?>) em.createQuery("select count(c.id) from CandidatExamen c join c.eleve el where c.examen.anneeScolaire.id = :a "
                + "group by lower(el.nom), lower(el.prenom), el.dateNaissance having count(distinct el.id) > 1").setParameter("a", anneeId).getResultList()).size();
        int multiples = ((List<?>) em.createQuery("select count(c.id) from CandidatExamen c where c.examen.anneeScolaire.id = :a "
                + "group by c.examen.id, c.eleve.id having count(c.id) > 1").setParameter("a", anneeId).getResultList()).size();

        // ----- Notes par discipline (SQL agrégé) -----
        List<Discipline> disciplines = new ArrayList<>();
        java.util.Map<String, Object[]> stats = new java.util.TreeMap<>();
        for (Object[] r : (List<Object[]>) em.createNativeQuery(
                "SELECT coalesce(m.libelle, e.libelle, 'Épreuve') AS discipline, count(*), avg(n.note), min(n.note), max(n.note) "
                        + "FROM note_examen n JOIN epreuve_examen e ON e.id = n.epreuve_id LEFT JOIN matiere m ON m.id = e.matiere_id "
                        + "JOIN examen_national x ON x.id = e.examen_id WHERE x.annee_scolaire_id = :a GROUP BY 1").setParameter("a", anneeId).getResultList()) {
            stats.put((String) r[0], r);
        }
        java.util.Map<String, Integer> manquantes = new java.util.TreeMap<>();
        for (Object[] r : (List<Object[]>) em.createNativeQuery(
                "SELECT coalesce(m.libelle, e.libelle, 'Épreuve') AS discipline, count(*) "
                        + "FROM epreuve_examen e LEFT JOIN matiere m ON m.id = e.matiere_id JOIN examen_national x ON x.id = e.examen_id "
                        + "JOIN candidat_examen c ON c.examen_id = e.examen_id AND c.statut_epreuves = 'PRESENT' "
                        + "LEFT JOIN note_examen n ON n.candidat_id = c.id AND n.epreuve_id = e.id "
                        + "WHERE n.candidat_id IS NULL AND x.annee_scolaire_id = :a GROUP BY 1").setParameter("a", anneeId).getResultList()) {
            manquantes.put((String) r[0], entier(r[1]));
        }
        java.util.Set<String> noms = new java.util.TreeSet<>(stats.keySet());
        noms.addAll(manquantes.keySet());
        int totalManquantes = 0;
        for (String nom : noms) {
            Object[] s = stats.get(nom);
            int m = manquantes.getOrDefault(nom, 0);
            totalManquantes += m;
            disciplines.add(new Discipline(nom, s == null ? 0 : entier(s[1]), s == null ? null : decimal(s[2]), s == null ? null : decimal(s[3]),
                    s == null ? null : decimal(s[4]), m));
        }

        // ----- Distribution des moyennes des présents, en fraction du barème -----
        Object[] d = (Object[]) em.createNativeQuery(
                "SELECT count(*) FILTER (WHERE c.moyenne < 0.25 * x.bareme), "
                        + "count(*) FILTER (WHERE c.moyenne >= 0.25 * x.bareme AND c.moyenne < 0.5 * x.bareme), "
                        + "count(*) FILTER (WHERE c.moyenne >= 0.5 * x.bareme AND c.moyenne < 0.75 * x.bareme), "
                        + "count(*) FILTER (WHERE c.moyenne >= 0.75 * x.bareme) "
                        + "FROM candidat_examen c JOIN examen_national x ON x.id = c.examen_id "
                        + "WHERE x.annee_scolaire_id = :a AND c.statut_epreuves = 'PRESENT' AND c.moyenne IS NOT NULL").setParameter("a", anneeId).getSingleResult();
        int[] effectifs = {entier(d[0]), entier(d[1]), entier(d[2]), entier(d[3])};
        return new SuiviTraitementDto(anneeId, correction, fraudes, new SuiviTraitementDto.Fiabilite(identites, multiples, totalManquantes), disciplines,
                tranches(effectifs));
    }
}
