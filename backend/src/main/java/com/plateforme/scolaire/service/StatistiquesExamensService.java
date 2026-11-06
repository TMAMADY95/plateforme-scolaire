package com.plateforme.scolaire.service;

import com.plateforme.scolaire.web.dto.StatistiquesExamensDto;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Statistiques réservées aux examens (DGECS et ministère) : les salles et centres d'examen, puis les résultats de chaque
 * niveau d'examen, au total et par type d'établissement. Ne porte volontairement sur aucune autre donnée du réseau scolaire.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatistiquesExamensService {

    static final int CANDIDATS_PAR_SALLE = 30;
    static final String SANS_NIVEAU = "Niveau non précisé";
    static final String SANS_TYPE = "Type non précisé";

    private final EntityManager em;

    /** Pur : pourcentage d'admis parmi les présents, arrondi au dixième ; null sans présent. */
    static Double taux(int admis, int presentes) {
        return presentes <= 0 ? null : BigDecimal.valueOf(admis * 100.0 / presentes).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    /** Pur : pourcentage de places utilisées dans les salles d'examen ; null sans salle utilisée. */
    static Double remplissage(int candidats, int salles) {
        return salles <= 0 ? null : BigDecimal.valueOf(candidats * 100.0 / (salles * CANDIDATS_PAR_SALLE)).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private static StatistiquesExamensDto.Resultats resultats(String libelle, int etablissements, int inscrits, int presentes, int admis,
            int absents, int elimines) {
        return new StatistiquesExamensDto.Resultats(libelle, etablissements, inscrits, presentes, admis, absents, elimines, taux(admis, presentes));
    }

    /** Pur : cumule des lignes de résultats. */
    static StatistiquesExamensDto.Resultats cumuler(String libelle, List<StatistiquesExamensDto.Resultats> lignes) {
        return resultats(libelle, lignes.stream().mapToInt(StatistiquesExamensDto.Resultats::etablissements).sum(),
                lignes.stream().mapToInt(StatistiquesExamensDto.Resultats::inscrits).sum(),
                lignes.stream().mapToInt(StatistiquesExamensDto.Resultats::presentes).sum(),
                lignes.stream().mapToInt(StatistiquesExamensDto.Resultats::admis).sum(),
                lignes.stream().mapToInt(StatistiquesExamensDto.Resultats::absents).sum(),
                lignes.stream().mapToInt(StatistiquesExamensDto.Resultats::elimines).sum());
    }

    /** Pur : libellé lisible d'une série du lycée. */
    static String libelleSerie(String code) {
        return switch (code == null ? "" : code) {
            case "SCIENCES_MATHEMATIQUES" -> "Sciences mathématiques";
            case "SCIENCES_SOCIALES" -> "Sciences sociales";
            case "SCIENCES_EXPERIMENTALES" -> "Sciences expérimentales";
            default -> code == null ? "Série non précisée" : code;
        };
    }

    private static StatistiquesExamensDto.Serie serie(String libelle, int inscrits, int presentes, int admis, List<StatistiquesExamensDto.Serie> parType) {
        return new StatistiquesExamensDto.Serie(libelle, inscrits, presentes, admis, taux(admis, presentes), parType);
    }

    /** Pur : séries d'un niveau avec leur détail par type d'établissement ; liste vide sans série. */
    static List<StatistiquesExamensDto.Serie> seriesDuNiveau(Map<String, Map<String, int[]>> parSerie) {
        if (parSerie == null) {
            return List.of();
        }
        return parSerie.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(e -> {
            List<StatistiquesExamensDto.Serie> types = e.getValue().entrySet().stream().sorted(Map.Entry.comparingByKey())
                    .map(t -> serie(t.getKey(), t.getValue().length > 2 ? t.getValue()[2] : 0, t.getValue()[0], t.getValue()[1], List.of())).toList();
            return serie(e.getKey(), types.stream().mapToInt(StatistiquesExamensDto.Serie::inscrits).sum(),
                    types.stream().mapToInt(StatistiquesExamensDto.Serie::presentes).sum(),
                    types.stream().mapToInt(StatistiquesExamensDto.Serie::admis).sum(), types);
        }).toList();
    }

    private static int entier(Object o) {
        return o == null ? 0 : ((Number) o).intValue();
    }

    @SuppressWarnings("unchecked")
    public StatistiquesExamensDto statistiques(Long anneeScolaireId) {
        // ----- Résultats par niveau d'examen et type d'établissement -----
        List<Object[]> resultatsBruts = em.createQuery(
                "select coalesce(n.libelle, '" + SANS_NIVEAU + "'), coalesce(n.ordre, 9999), coalesce(t.libelle, '" + SANS_TYPE + "'), "
                        + "count(distinct e.id), sum(r.presentes), sum(r.admis), sum(r.absents), sum(r.elimines) "
                        + "from ResultatExamen r join r.examen x left join x.niveau n join r.etablissement e left join e.typeEtablissement t "
                        + "where x.anneeScolaire.id = :a group by n.libelle, n.ordre, t.libelle").setParameter("a", anneeScolaireId).getResultList();
        List<Object[]> inscritsBruts = em.createQuery(
                "select coalesce(n.libelle, '" + SANS_NIVEAU + "'), coalesce(n.ordre, 9999), coalesce(t.libelle, '" + SANS_TYPE + "'), count(c.id) "
                        + "from CandidatExamen c join c.examen x left join x.niveau n join c.etablissement e left join e.typeEtablissement t "
                        + "where x.anneeScolaire.id = :a group by n.libelle, n.ordre, t.libelle").setParameter("a", anneeScolaireId).getResultList();
        List<Object[]> examensParNiveau = em.createQuery(
                "select coalesce(n.libelle, '" + SANS_NIVEAU + "'), coalesce(n.ordre, 9999), count(x.id) from ExamenNational x left join x.niveau n "
                        + "where x.anneeScolaire.id = :a group by n.libelle, n.ordre").setParameter("a", anneeScolaireId).getResultList();

        List<Object[]> seriesBrutes = em.createQuery(
                "select coalesce(n.libelle, '" + SANS_NIVEAU + "'), coalesce(t.libelle, '" + SANS_TYPE + "'), s.serie, sum(s.presentes), sum(s.admis) "
                        + "from ResultatExamen r join r.examen x left join x.niveau n join r.etablissement e left join e.typeEtablissement t join r.series s "
                        + "where x.anneeScolaire.id = :a group by n.libelle, t.libelle, s.serie").setParameter("a", anneeScolaireId).getResultList();
        // niveau -> série -> type -> {présents, admis}
        Map<String, Map<String, Map<String, int[]>>> series = new HashMap<>();
        for (Object[] r : seriesBrutes) {
            series.computeIfAbsent((String) r[0], k -> new LinkedHashMap<>()).computeIfAbsent(libelleSerie((String) r[2]), k -> new LinkedHashMap<>())
                    .merge((String) r[1], new int[] {entier(r[3]), entier(r[4]), 0}, (x, y) -> new int[] {x[0] + y[0], x[1] + y[1], 0});
        }
        List<Object[]> inscritsSeries = em.createQuery(
                "select coalesce(n.libelle, '" + SANS_NIVEAU + "'), coalesce(t.libelle, '" + SANS_TYPE + "'), c.serie, count(c.id) "
                        + "from CandidatExamen c join c.examen x left join x.niveau n join c.etablissement e left join e.typeEtablissement t "
                        + "where x.anneeScolaire.id = :a and c.serie is not null group by n.libelle, t.libelle, c.serie").setParameter("a", anneeScolaireId).getResultList();
        for (Object[] r : inscritsSeries) {
            series.computeIfAbsent((String) r[0], k -> new LinkedHashMap<>()).computeIfAbsent(libelleSerie((String) r[2]), k -> new LinkedHashMap<>())
                    .merge((String) r[1], new int[] {0, 0, entier(r[3])}, (x, y) -> new int[] {x[0], x[1], y[2]});
        }
        Map<String, Integer> ordres = new HashMap<>();
        Map<String, Integer> nbExamens = new HashMap<>();
        for (Object[] r : examensParNiveau) {
            ordres.put((String) r[0], entier(r[1]));
            nbExamens.put((String) r[0], entier(r[2]));
        }
        // clé niveau|type -> {étab, présentes, admis, absents, éliminés, inscrits}
        Map<String, int[]> cumul = new LinkedHashMap<>();
        for (Object[] r : resultatsBruts) {
            ordres.putIfAbsent((String) r[0], entier(r[1]));
            int[] v = cumul.computeIfAbsent(r[0] + "|" + r[2], k -> new int[6]);
            v[0] = entier(r[3]);
            v[1] = entier(r[4]);
            v[2] = entier(r[5]);
            v[3] = entier(r[6]);
            v[4] = entier(r[7]);
        }
        for (Object[] r : inscritsBruts) {
            ordres.putIfAbsent((String) r[0], entier(r[1]));
            cumul.computeIfAbsent(r[0] + "|" + r[2], k -> new int[6])[5] = entier(r[3]);
        }

        Map<String, List<StatistiquesExamensDto.Resultats>> parNiveau = new LinkedHashMap<>();
        Map<String, List<StatistiquesExamensDto.Resultats>> parTypeGlobal = new LinkedHashMap<>();
        for (Map.Entry<String, int[]> e : cumul.entrySet()) {
            String[] cle = e.getKey().split("\\|", 2);
            int[] v = e.getValue();
            var ligne = resultats(cle[1], v[0], v[5], v[1], v[2], v[3], v[4]);
            parNiveau.computeIfAbsent(cle[0], k -> new ArrayList<>()).add(ligne);
            parTypeGlobal.computeIfAbsent(cle[1], k -> new ArrayList<>()).add(ligne);
        }
        for (String niveau : nbExamens.keySet()) {
            parNiveau.computeIfAbsent(niveau, k -> new ArrayList<>());
        }
        List<StatistiquesExamensDto.Niveau> niveaux = parNiveau.entrySet().stream()
                .sorted(java.util.Comparator.comparingInt((Map.Entry<String, List<StatistiquesExamensDto.Resultats>> x) ->
                        ordres.getOrDefault(x.getKey(), 9999)).thenComparing(Map.Entry::getKey))
                .map(x -> new StatistiquesExamensDto.Niveau(x.getKey(), nbExamens.getOrDefault(x.getKey(), 0), cumuler(x.getKey(), x.getValue()),
                        x.getValue().stream().sorted(java.util.Comparator.comparing(StatistiquesExamensDto.Resultats::libelle)).toList(),
                        seriesDuNiveau(series.get(x.getKey()))))
                .toList();
        List<StatistiquesExamensDto.Resultats> types = parTypeGlobal.entrySet().stream()
                .map(x -> cumuler(x.getKey(), x.getValue())).sorted(java.util.Comparator.comparing(StatistiquesExamensDto.Resultats::libelle)).toList();

        // ----- Centres et salles d'examen -----
        Map<Long, int[]> sallesPhysiques = new HashMap<>();
        for (Object[] r : (List<Object[]>) em.createQuery("select s.etablissement.id, count(s.id), coalesce(sum(s.capacite), 0) from Salle s group by s.etablissement.id")
                .getResultList()) {
            sallesPhysiques.put(((Number) r[0]).longValue(), new int[] {entier(r[1]), entier(r[2])});
        }
        List<Object[]> centresBruts = em.createQuery(
                "select ce.id, ce.nom, z.nom, x.libelle, count(c.id), count(distinct c.salle), ce.etablissement.id, count(c.salle) "
                        + "from CentreExamen ce join ce.examen x left join ce.zone z left join CandidatExamen c on c.centre = ce "
                        + "where x.anneeScolaire.id = :a group by ce.id, ce.nom, z.nom, x.libelle, ce.etablissement.id order by x.libelle, ce.nom")
                .setParameter("a", anneeScolaireId).getResultList();
        List<StatistiquesExamensDto.LigneCentre> centres = new ArrayList<>();
        int sallesUtilisees = 0;
        int affectes = 0;
        // Seuls les candidats placés en salle occupent une place : un candidat affecté à un centre peut ne pas encore avoir de salle.
        int places = 0;
        int sallesDispo = 0;
        int capacite = 0;
        for (Object[] r : centresBruts) {
            int candidats = entier(r[4]);
            int salles = entier(r[5]);
            int enSalle = entier(r[7]);
            int[] phys = r[6] == null ? null : sallesPhysiques.get(((Number) r[6]).longValue());
            centres.add(new StatistiquesExamensDto.LigneCentre(((Number) r[0]).longValue(), (String) r[1], (String) r[2], (String) r[3], candidats,
                    salles, phys == null ? null : phys[0], phys == null ? null : phys[1], remplissage(enSalle, salles)));
            sallesUtilisees += salles;
            affectes += candidats;
            places += enSalle;
            if (phys != null) {
                sallesDispo += phys[0];
                capacite += phys[1];
            }
        }
        int sansCentre = entier(em.createQuery("select count(c.id) from CandidatExamen c where c.examen.anneeScolaire.id = :a and c.centre is null")
                .setParameter("a", anneeScolaireId).getSingleResult());
        var salles = new StatistiquesExamensDto.Salles(centres.size(), sallesUtilisees, affectes, sansCentre, sallesDispo, capacite,
                remplissage(places, sallesUtilisees));
        List<StatistiquesExamensDto.LigneLycee> lycee = ((List<Object[]>) em.createQuery(
                "select n.libelle, n.ordre, cl.serie, count(distinct cl.id), count(distinct i.id) from Inscription i join i.classe cl join cl.niveau n "
                        + "where cl.anneeScolaire.id = :a and cl.serie is not null and i.statut in ('EN_ATTENTE', 'ACCEPTEE') "
                        + "group by n.libelle, n.ordre, cl.serie order by n.ordre, cl.serie").setParameter("a", anneeScolaireId).getResultList()).stream()
                .map(r -> new StatistiquesExamensDto.LigneLycee((String) r[0], libelleSerie((String) r[2]), entier(r[3]), entier(r[4]))).toList();
        return new StatistiquesExamensDto(anneeScolaireId, salles, centres, niveaux, types, lycee);
    }
}
