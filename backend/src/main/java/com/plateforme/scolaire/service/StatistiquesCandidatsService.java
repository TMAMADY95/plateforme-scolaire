package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.BaremeExamen;
import com.plateforme.scolaire.web.dto.StatistiquesCandidatsDto;
import com.plateforme.scolaire.web.dto.StatistiquesCandidatsDto.Ligne;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Profil des candidats (filles/garçons, libres/scolaires, zone), participation (présents, absents, éliminés), réussite
 * (sur les présents et sur les inscrits, filles et garçons), mentions et évolution par rapport à l'année précédente.
 * Les examens relèvent de la DGECS ; le ministère ne fait que consulter.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatistiquesCandidatsService {

    static final String SANS_NIVEAU = "Niveau non précisé";
    static final String SANS_ZONE = "Zone non précisée";
    static final List<String> MENTIONS = List.of("TRES_BIEN", "BIEN", "ASSEZ_BIEN", "PASSABLE");

    private final EntityManager em;

    /** Un candidat réduit à ce qui compte pour les statistiques. */
    record Candidat(String niveau, String sexe, boolean libre, String statut, String decision, BigDecimal moyenne, int bareme, String zone,
            String typeZone, String region, String prefecture) {
    }

    static final String SANS_REGION = "Région non précisée";
    static final String SANS_PREFECTURE = "Préfecture non précisée";

    /** Pur : nom de l'ancêtre (ou de la zone elle-même) du type demandé parmi la zone, son parent et son grand-parent ; null sinon. */
    static String ancetre(String type, String[] zone, String[] parent, String[] grandParent) {
        for (String[] z : new String[][] {zone, parent, grandParent}) {
            if (z != null && type.equals(z[1]) && z[0] != null) {
                return z[0];
            }
        }
        return null;
    }

    /** Cumul modifiable d'un groupe de candidats. */
    static final class Cumul {
        int inscrits, filles, garcons, libres, presents, absents, elimines, admis, presentsFilles, admisFilles;

        void ajouter(Candidat c) {
            inscrits++;
            boolean fille = "F".equals(c.sexe());
            if (fille) {
                filles++;
            } else if ("M".equals(c.sexe())) {
                garcons++;
            }
            if (c.libre()) {
                libres++;
            }
            switch (c.statut() == null ? "" : c.statut()) {
                case "PRESENT" -> {
                    presents++;
                    presentsFilles += fille ? 1 : 0;
                    if ("ADMIS".equals(c.decision())) {
                        admis++;
                        admisFilles += fille ? 1 : 0;
                    }
                }
                case "ABSENT" -> absents++;
                case "ELIMINE" -> elimines++;
                default -> { }
            }
        }
    }

    /** Pur : pourcentage au dixième ; null si le dénominateur est nul. */
    static Double pct(int partie, int total) {
        return total <= 0 ? null : BigDecimal.valueOf(partie * 100.0 / total).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    /** Pur : pourcentage au dixième de deux montants ; null si le dénominateur est nul. */
    static Double pct10(long partie, long total) {
        return total <= 0 ? null : BigDecimal.valueOf(partie * 100.0 / total).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    /** Pur : variation en pourcentage entre deux effectifs ; null sans année précédente. */
    static Double evolution(int actuel, Integer precedent) {
        return precedent == null || precedent <= 0 ? null
                : BigDecimal.valueOf((actuel - precedent) * 100.0 / precedent).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    /** Pur : transforme un cumul en ligne, avec la comparaison éventuelle à l'année précédente. */
    static Ligne ligne(String libelle, String detail, Cumul c, Cumul precedent) {
        return new Ligne(libelle, detail, c.inscrits, c.filles, c.garcons, c.libres, c.inscrits - c.libres, c.presents, c.absents, c.elimines, c.admis,
                c.presentsFilles, c.admisFilles, pct(c.filles, c.inscrits), pct(c.libres, c.inscrits), pct(c.presents, c.inscrits),
                pct(c.absents, c.inscrits), pct(c.admis, c.presents), pct(c.admis, c.inscrits), pct(c.admisFilles, c.presentsFilles),
                pct(c.admis - c.admisFilles, c.presents - c.presentsFilles), precedent == null ? null : precedent.inscrits,
                precedent == null ? null : pct(precedent.admis, precedent.presents), evolution(c.inscrits, precedent == null ? null : precedent.inscrits));
    }

    /** Pur : cumule les candidats par clé de groupe. */
    static Map<String, Cumul> grouper(List<Candidat> candidats, java.util.function.Function<Candidat, String> cle) {
        Map<String, Cumul> res = new LinkedHashMap<>();
        for (Candidat c : candidats) {
            res.computeIfAbsent(cle.apply(c), k -> new Cumul()).ajouter(c);
        }
        return res;
    }

    /** Pur : répartition des mentions parmi les admis (toujours les quatre mentions, dans l'ordre). */
    static List<StatistiquesCandidatsDto.Mention> mentions(List<Candidat> candidats) {
        Map<String, Integer> nb = new LinkedHashMap<>();
        MENTIONS.forEach(m -> nb.put(m, 0));
        int admis = 0;
        for (Candidat c : candidats) {
            if ("PRESENT".equals(c.statut()) && "ADMIS".equals(c.decision())) {
                admis++;
                nb.merge(BaremeExamen.mention(c.moyenne(), c.bareme()), 1, Integer::sum);
            }
        }
        int total = admis;
        return nb.entrySet().stream().map(e -> new StatistiquesCandidatsDto.Mention(e.getKey(), e.getValue(), pct(e.getValue(), total))).toList();
    }

    /** Pur : assemble le résultat à partir des candidats de l'année et de ceux de l'année précédente. */
    static StatistiquesCandidatsDto assembler(Long annee, Long precedente, List<Candidat> actuels, List<Candidat> anterieurs) {
        Map<String, Cumul> avant = grouper(anterieurs, Candidat::niveau);
        List<Ligne> niveaux = grouper(actuels, Candidat::niveau).entrySet().stream()
                .sorted(Map.Entry.comparingByKey()).map(e -> ligne(e.getKey(), null, e.getValue(), precedente == null ? null : avant.get(e.getKey()))).toList();
        Map<String, String> typesZone = new LinkedHashMap<>();
        actuels.forEach(c -> typesZone.putIfAbsent(c.zone(), c.typeZone()));
        List<Ligne> zones = grouper(actuels, Candidat::zone).entrySet().stream().sorted(Comparator.comparing(
                (Map.Entry<String, Cumul> e) -> e.getValue().inscrits).reversed().thenComparing(Map.Entry::getKey))
                .map(e -> ligne(e.getKey(), typesZone.get(e.getKey()), e.getValue(), null)).toList();
        List<Ligne> regions = grouper(actuels, c -> c.region() == null ? SANS_REGION : c.region()).entrySet().stream()
                .sorted(Map.Entry.comparingByKey()).map(e -> ligne(e.getKey(), null, e.getValue(), null)).toList();
        List<Ligne> prefectures = grouper(actuels, c -> c.prefecture() == null ? SANS_PREFECTURE : c.prefecture()).entrySet().stream()
                .sorted(Map.Entry.comparingByKey()).map(e -> ligne(e.getKey(), null, e.getValue(), null)).toList();
        Cumul total = new Cumul();
        actuels.forEach(total::ajouter);
        Cumul totalAvant = null;
        if (precedente != null) {
            totalAvant = new Cumul();
            anterieurs.forEach(totalAvant::ajouter);
        }
        return new StatistiquesCandidatsDto(annee, precedente, ligne("Tous les examens", null, total, totalAvant), niveaux, zones, regions, prefectures, mentions(actuels));
    }

    @SuppressWarnings("unchecked")
    private List<Candidat> candidats(Long anneeId) {
        List<Object[]> rows = em.createQuery(
                "select coalesce(n.libelle, '" + SANS_NIVEAU + "'), el.sexe, c.libre, c.statutEpreuves, c.decision, c.moyenne, x.bareme, "
                        + "coalesce(z.nom, '" + SANS_ZONE + "'), z.typeZone, p.nom, p.typeZone, g.nom, g.typeZone "
                        + "from CandidatExamen c join c.examen x left join x.niveau n join c.eleve el left join c.etablissement e left join e.zone z "
                        + "left join z.parent p left join p.parent g "
                        + "where x.anneeScolaire.id = :a").setParameter("a", anneeId).getResultList();
        List<Candidat> res = new ArrayList<>(rows.size());
        for (Object[] r : rows) {
            res.add(new Candidat((String) r[0], r[1] == null ? null : r[1].toString(), Boolean.TRUE.equals(r[2]), (String) r[3], (String) r[4],
                    (BigDecimal) r[5], r[6] == null ? BaremeExamen.SUR_20 : ((Number) r[6]).intValue(), (String) r[7], (String) r[8],
                    ancetre("REGION", new String[] {(String) r[7], (String) r[8]}, new String[] {(String) r[9], (String) r[10]}, new String[] {(String) r[11], (String) r[12]}),
                    ancetre("PREFECTURE", new String[] {(String) r[7], (String) r[8]}, new String[] {(String) r[9], (String) r[10]}, new String[] {(String) r[11], (String) r[12]})));
        }
        return res;
    }

    public StatistiquesCandidatsDto statistiques(Long anneeScolaireId) {
        List<?> prec = em.createQuery("select a.id from AnneeScolaire a where a.dateDebut < (select b.dateDebut from AnneeScolaire b where b.id = :a) "
                + "order by a.dateDebut desc").setParameter("a", anneeScolaireId).setMaxResults(1).getResultList();
        Long precedente = prec.isEmpty() ? null : ((Number) prec.get(0)).longValue();
        return assembler(anneeScolaireId, precedente, candidats(anneeScolaireId), precedente == null ? List.of() : candidats(precedente));
    }
}
