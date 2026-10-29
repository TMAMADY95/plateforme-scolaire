package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.Etablissement;
import com.plateforme.scolaire.repository.ClasseRepository;
import com.plateforme.scolaire.repository.EtablissementRepository;
import com.plateforme.scolaire.repository.InscriptionRepository;
import com.plateforme.scolaire.repository.StatistiquesRepository;
import com.plateforme.scolaire.web.dto.StatistiquesDto;
import com.plateforme.scolaire.web.dto.StatistiquesDto.PointMois;
import com.plateforme.scolaire.web.dto.StatistiquesDto.Repartition;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
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
        return calculer(anneeScolaireId,
                etablissementRepository.findAll().stream().map(Etablissement::getId).toList());
    }

    public StatistiquesDto regionales(Long anneeScolaireId) {
        return calculer(anneeScolaireId, List.copyOf(regionService.etablissementsDeLaRegion().keySet()));
    }

    public StatistiquesDto etablissement(Long etablissementId, Long anneeScolaireId) {
        return calculer(anneeScolaireId, List.of(etablissementId));
    }

    private StatistiquesDto calculer(Long annee, Collection<Long> ids) {
        if (ids.isEmpty()) {
            return new StatistiquesDto(annee, 0, 0, 0, 0, null, JOURS_PRESENCE, List.of(), List.of(), List.of(),
                    List.of());
        }
        return new StatistiquesDto(annee, ids.size(),
                statistiquesRepository.compterEnseignants(ids),
                classeRepository.countByEtablissementIdInAndAnneeScolaireId(ids, annee),
                inscriptionRepository.countByEtablissementIdInAndAnneeScolaireIdAndStatut(ids, annee, "ACCEPTEE"),
                tauxPresence(statistiquesRepository.presencesDepuis(ids,
                        LocalDate.now(clock).minusDays(JOURS_PRESENCE))),
                JOURS_PRESENCE,
                repartitions(statistiquesRepository.repartitionParType(ids)),
                repartitions(statistiquesRepository.effectifsParNiveau(ids, annee)),
                repartitions(statistiquesRepository.effectifsParZone(ids, annee)),
                statistiquesRepository.inscriptionsParMois(ids, annee).stream()
                        .map(l -> new PointMois(mois(((Number) l[0]).intValue(), ((Number) l[1]).intValue()),
                                ((Number) l[2]).longValue()))
                        .toList());
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
