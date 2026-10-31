package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.Etablissement;
import com.plateforme.scolaire.domain.Role;
import com.plateforme.scolaire.domain.ZoneGeographique;
import com.plateforme.scolaire.repository.ClasseRepository;
import com.plateforme.scolaire.repository.EtablissementRepository;
import com.plateforme.scolaire.repository.InscriptionRepository;
import com.plateforme.scolaire.repository.StatistiquesRepository;
import com.plateforme.scolaire.web.dto.LigneEtablissementStatDto;
import com.plateforme.scolaire.web.dto.LigneZoneStatDto;
import com.plateforme.scolaire.web.dto.StatistiquesDto;
import com.plateforme.scolaire.web.dto.StatistiquesDto.PointMois;
import com.plateforme.scolaire.web.dto.StatistiquesDto.Repartition;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Statistiques des tableaux de bord pour un ensemble d'établissements (le pays, une région ou un seul). */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatistiquesService {

    static final int JOURS_PRESENCE = 30;

    private final StatistiquesRepository statistiquesRepository;
    private final EtablissementRepository etablissementRepository;
    private final ClasseRepository classeRepository;
    private final InscriptionRepository inscriptionRepository;
    private final RegionService regionService;
    private final Clock clock;

    public StatistiquesDto nationales(Long anneeScolaireId) {
        return calculer(anneeScolaireId, idsNationaux());
    }

    /** Une zone (région, département…) et ses sous-zones, vue par le ministère. */
    public StatistiquesDto zone(Long zoneId, Long anneeScolaireId) {
        return calculer(anneeScolaireId, idsDeLaZone(zoneId));
    }

    public StatistiquesDto regionales(Long anneeScolaireId) {
        return calculer(anneeScolaireId, List.copyOf(regionService.etablissementsDeLaRegion().keySet()));
    }

    public StatistiquesDto etablissement(Long etablissementId, Long anneeScolaireId) {
        return calculer(anneeScolaireId, List.of(etablissementId));
    }

    /** Une ligne par établissement du pays, ou d'une zone si [zoneId] est donné. */
    public List<LigneEtablissementStatDto> lignesNationales(Long anneeScolaireId, Long zoneId) {
        return lignes(anneeScolaireId, zoneId == null ? idsNationaux() : idsDeLaZone(zoneId));
    }

    /** Une ligne par établissement de la région de l'utilisateur. */
    public List<LigneEtablissementStatDto> lignesRegionales(Long anneeScolaireId) {
        return lignes(anneeScolaireId, List.copyOf(regionService.etablissementsDeLaRegion().keySet()));
    }

    /** Une ligne par zone qui contient au moins un établissement (sous-zones comprises dans leurs zones parentes). */
    public List<LigneZoneStatDto> lignesParZone(Long anneeScolaireId) {
        Map<Long, long[]> totaux = new LinkedHashMap<>();
        Map<Long, ZoneGeographique> zones = new LinkedHashMap<>();
        List<Long> ids = idsNationaux();
        Map<Long, Etablissement> parId = new HashMap<>();
        etablissementRepository.findAllById(ids).forEach(e -> parId.put(e.getId(), e));
        for (LigneEtablissementStatDto l : lignes(anneeScolaireId, ids)) {
            Etablissement e = parId.get(l.id());
            Set<Long> vues = new HashSet<>();
            for (ZoneGeographique z = e == null ? null : e.getZone(); z != null && vues.add(z.getId()); z = z.getParent()) {
                zones.putIfAbsent(z.getId(), z);
                long[] t = totaux.computeIfAbsent(z.getId(), k -> new long[5]);
                t[0]++;
                t[1] += l.eleves();
                t[2] += l.enseignants();
                t[3] += l.personnelDirection();
                t[4] += l.classes();
            }
        }
        return totaux.entrySet().stream()
                .map(en -> {
                    ZoneGeographique z = zones.get(en.getKey());
                    long[] t = en.getValue();
                    return new LigneZoneStatDto(z.getId(), z.getNom(), z.getTypeZone(),
                            z.getParent() == null ? null : z.getParent().getId(), t[0], t[1], t[2], t[3], t[4]);
                })
                .sorted(Comparator.comparing(LigneZoneStatDto::nom, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private List<Long> idsNationaux() {
        return etablissementRepository.findAll().stream().map(Etablissement::getId).toList();
    }

    private List<Long> idsDeLaZone(Long zoneId) {
        return etablissementRepository.findByZoneEtSousZones(zoneId).stream().map(Etablissement::getId).toList();
    }

    private List<LigneEtablissementStatDto> lignes(Long annee, Collection<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<Long, Long> eleves = compter(statistiquesRepository.elevesParEtablissement(ids, annee));
        Map<Long, Long> classes = compter(statistiquesRepository.classesParEtablissement(ids, annee));
        Map<Long, Long> enseignants =
                compter(statistiquesRepository.personnelParEtablissement(ids, Role.ENSEIGNANT));
        Map<Long, Long> direction = compter(statistiquesRepository.directionParEtablissement(ids));
        return etablissementRepository.findAllById(ids).stream()
                .sorted(Comparator.comparing(Etablissement::getNom, String.CASE_INSENSITIVE_ORDER))
                .map(e -> LigneEtablissementStatDto.de(e, eleves.getOrDefault(e.getId(), 0L),
                        enseignants.getOrDefault(e.getId(), 0L), direction.getOrDefault(e.getId(), 0L),
                        classes.getOrDefault(e.getId(), 0L)))
                .toList();
    }

    private static Map<Long, Long> compter(List<Object[]> lignes) {
        Map<Long, Long> resultat = new HashMap<>();
        for (Object[] l : lignes) {
            resultat.put(((Number) l[0]).longValue(), ((Number) l[1]).longValue());
        }
        return resultat;
    }

    private StatistiquesDto calculer(Long annee, Collection<Long> ids) {
        if (ids.isEmpty()) {
            return new StatistiquesDto(annee, 0, 0, 0, 0, 0, null, JOURS_PRESENCE, List.of(), List.of(), List.of(),
                    List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
        }
        return new StatistiquesDto(annee, ids.size(),
                statistiquesRepository.compterEnseignants(ids),
                statistiquesRepository.compterPersonnelDirection(ids),
                classeRepository.countByEtablissementIdInAndAnneeScolaireId(ids, annee),
                inscriptionRepository.countByEtablissementIdInAndAnneeScolaireIdAndStatut(ids, annee, "ACCEPTEE"),
                tauxPresence(statistiquesRepository.presencesDepuis(ids,
                        LocalDate.now(clock).minusDays(JOURS_PRESENCE))),
                JOURS_PRESENCE,
                repartitions(statistiquesRepository.repartitionParType(ids)),
                repartitions(statistiquesRepository.repartitionParSecteur(ids)).stream()
                        .map(r -> new Repartition("PRIVE".equals(r.libelle()) ? "Privé" : "Public", r.valeur())).toList(),
                repartitions(statistiquesRepository.effectifsParCycle(ids, annee)),
                repartitions(statistiquesRepository.effectifsParNiveau(ids, annee)),
                repartitions(statistiquesRepository.effectifsParSexe(ids, annee)).stream()
                        .map(r -> new Repartition(libelleSexe(r.libelle()), r.valeur())).toList(),
                repartitions(statistiquesRepository.effectifsParZone(ids, annee)),
                statistiquesRepository.inscriptionsParMois(ids, annee).stream()
                        .map(l -> new PointMois(mois(((Number) l[0]).intValue(), ((Number) l[1]).intValue()),
                                ((Number) l[2]).longValue()))
                        .toList(),
                repartitions(statistiquesRepository.repartitionParTypeEtablissement(ids)),
                repartitions(statistiquesRepository.effectifsParTypeEtablissement(ids, annee)));
    }

    static String libelleSexe(String sexe) {
        if ("F".equals(sexe)) {
            return "Filles";
        }
        return "M".equals(sexe) ? "Garçons" : "Non précisé";
    }

    private static List<Repartition> repartitions(List<Object[]> lignes) {
        return lignes.stream().map(l -> new Repartition((String) l[0], ((Number) l[1]).longValue())).toList();
    }

    static String mois(int annee, int mois) {
        return String.format("%04d-%02d", annee, mois);
    }

    /** (présents + retards) / total, en pourcentage à une décimale ; null sans aucune présence saisie. */
    static BigDecimal tauxPresence(List<Object[]> parStatut) {
        long total = 0;
        long presents = 0;
        for (Object[] l : parStatut) {
            long nombre = ((Number) l[1]).longValue();
            total += nombre;
            if (!"ABSENT".equals(l[0])) {
                presents += nombre;
            }
        }
        if (total == 0) {
            return null;
        }
        return BigDecimal.valueOf(presents * 100).divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);
    }
}
