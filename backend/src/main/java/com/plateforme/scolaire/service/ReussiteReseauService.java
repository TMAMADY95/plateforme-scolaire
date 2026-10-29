package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.Etablissement;
import com.plateforme.scolaire.repository.EtablissementRepository;
import com.plateforme.scolaire.repository.ReussiteReseauRepository;
import com.plateforme.scolaire.service.CalculMoyennes.NoteCalcul;
import com.plateforme.scolaire.web.dto.ReussiteReseauDto;
import com.plateforme.scolaire.web.dto.ReussiteReseauDto.Groupe;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Taux de réussite national ou régional. Les moyennes sont recalculées avec {@link CalculMoyennes}, comme pour les
 * bulletins (coefficients de matières compris), par lots d'établissements pour borner la mémoire utilisée.
 *
 * <p>Le calcul est lourd à l'échelle d'un pays : le résultat est gardé en mémoire quelques minutes
 * ({@code app.reussite.cache-minutes}, 0 = pas de cache). Une note saisie peut donc mettre ce délai à apparaître
 * dans ces vues. Le cache est propre à chaque instance du serveur.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReussiteReseauService {

    static final int LOT = 100;

    private final ReussiteReseauRepository repository;
    private final EtablissementRepository etablissementRepository;
    private final RegionService regionService;

    @Value("${app.reussite.cache-minutes:10}")
    private long cacheMinutes = 10;

    private record Entree(Instant calculeLe, ReussiteReseauDto dto) {
    }

    private final Map<String, Entree> cache = new ConcurrentHashMap<>();

    public ReussiteReseauDto nationale(Long anneeScolaireId, Long periodeId) {
        Map<Long, Etablissement> etablissements = new LinkedHashMap<>();
        etablissementRepository.findAll().forEach(e -> etablissements.put(e.getId(), e));
        return calculer(anneeScolaireId, periodeId, etablissements);
    }

    public ReussiteReseauDto regionale(Long anneeScolaireId, Long periodeId) {
        return calculer(anneeScolaireId, periodeId, regionService.etablissementsDeLaRegion());
    }

    /** Compteur d'élèves notés et d'élèves ayant réussi. */
    private static final class Cumul {
        final String libelle;
        long evalues;
        long admis;

        Cumul(String libelle) {
            this.libelle = libelle;
        }

        void ajouter(long evaluesEnPlus, long admisEnPlus) {
            evalues += evaluesEnPlus;
            admis += admisEnPlus;
        }
    }

    private ReussiteReseauDto calculer(Long annee, Long periode, Map<Long, Etablissement> etablissements) {
        if (cacheMinutes <= 0) {
            return calculerSansCache(annee, periode, etablissements);
        }
        String cle = annee + "|" + periode + "|" + etablissements.size() + "|" + etablissements.keySet().hashCode();
        Instant maintenant = Instant.now();
        Entree entree = cache.get(cle);
        if (entree != null && entree.calculeLe().plus(Duration.ofMinutes(cacheMinutes)).isAfter(maintenant)) {
            return entree.dto();
        }
        ReussiteReseauDto dto = calculerSansCache(annee, periode, etablissements);
        if (cache.size() >= 200) {
            cache.clear();
        }
        cache.put(cle, new Entree(maintenant, dto));
        return dto;
    }

    private ReussiteReseauDto calculerSansCache(Long annee, Long periode, Map<Long, Etablissement> etablissements) {
        Map<String, Cumul> parNiveau = new TreeMap<>();
        Map<String, Cumul> parZone = new LinkedHashMap<>();
        Map<Long, Cumul> parEtablissement = new LinkedHashMap<>();
        long evalues = 0;
        long admis = 0;

        List<Long> ids = List.copyOf(etablissements.keySet());
        for (int debut = 0; debut < ids.size(); debut += LOT) {
            List<Long> lot = ids.subList(debut, Math.min(ids.size(), debut + LOT));

            Map<Long, Object[]> classes = new HashMap<>();
            repository.classes(lot, annee).forEach(l -> classes.put(((Number) l[0]).longValue(), l));
            Map<Long, Map<Long, BigDecimal>> coefficients = new HashMap<>();
            repository.coefficients(lot).forEach(l -> coefficients
                    .computeIfAbsent(((Number) l[0]).longValue(), k -> new HashMap<>())
                    .put(((Number) l[1]).longValue(), (BigDecimal) l[2]));
            Map<Long, List<NoteCalcul>> notesParClasse = new HashMap<>();
            for (Object[] l : repository.notes(lot, periode)) {
                long classeId = ((Number) l[0]).longValue();
                notesParClasse.computeIfAbsent(classeId, k -> new ArrayList<>())
                        .add(new NoteCalcul(((Number) l[1]).longValue(), ((Number) l[2]).longValue(),
                                (BigDecimal) l[3], (BigDecimal) l[4], (BigDecimal) l[5]));
            }

            for (Map.Entry<Long, List<NoteCalcul>> e : notesParClasse.entrySet()) {
                Object[] classe = classes.get(e.getKey());
                if (classe == null) {
                    continue; // classe d'une autre année : ses évaluations ne comptent pas ici
                }
                Map<Long, BigDecimal> generales = CalculMoyennes
                        .calculer(e.getValue(), coefficients.getOrDefault(e.getKey(), Map.of()))
                        .moyennesGenerales();
                long reussis = generales.values().stream()
                        .filter(m -> CalculMoyennes.arrondir(m).compareTo(ReussiteService.SEUIL) >= 0).count();

                Etablissement etablissement = etablissements.get(((Number) classe[1]).longValue());
                String niveau = (String) classe[2];
                int ordre = ((Number) classe[3]).intValue();
                String zone = etablissement.getZone() == null ? "Sans zone" : etablissement.getZone().getNom();

                parNiveau.computeIfAbsent(String.format("%05d|%s", ordre, niveau), k -> new Cumul(niveau))
                        .ajouter(generales.size(), reussis);
                parZone.computeIfAbsent(zone, Cumul::new).ajouter(generales.size(), reussis);
                parEtablissement.computeIfAbsent(etablissement.getId(), k -> new Cumul(etablissement.getNom()))
                        .ajouter(generales.size(), reussis);
                evalues += generales.size();
                admis += reussis;
            }
        }
        return new ReussiteReseauDto(annee, periode, ReussiteService.SEUIL, evalues, admis,
                ReussiteService.taux(admis, evalues),
                groupes(parNiveau.values().stream().toList(), false),
                groupes(parZone.values().stream().toList(), true),
                groupes(parEtablissement.values().stream().toList(), true));
    }

    /** Un groupe par entrée ; triés du meilleur taux au moins bon quand [trier] est vrai. */
    private static List<Groupe> groupes(List<Cumul> cumuls, boolean trier) {
        List<Groupe> groupes = new ArrayList<>(cumuls.stream()
                .map(c -> new Groupe(c.libelle, c.evalues, c.admis, ReussiteService.taux(c.admis, c.evalues)))
                .toList());
        if (trier) {
            groupes.sort(Comparator.comparing((Groupe g) -> g.taux() == null ? BigDecimal.ZERO : g.taux()).reversed()
                    .thenComparing(Groupe::libelle));
        }
        return groupes;
    }
}
