package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.AnneeScolaire;
import com.plateforme.scolaire.domain.Etablissement;
import com.plateforme.scolaire.repository.AnneeScolaireRepository;
import com.plateforme.scolaire.repository.ClasseRepository;
import com.plateforme.scolaire.repository.EtablissementRepository;
import com.plateforme.scolaire.repository.InscriptionRepository;
import com.plateforme.scolaire.repository.ResultatExamenRepository;
import com.plateforme.scolaire.web.dto.EvolutionDto;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Compare les années scolaires : élèves inscrits, classes et taux de réussite aux examens nationaux. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EvolutionService {

    private final AnneeScolaireRepository anneeRepository;
    private final EtablissementRepository etablissementRepository;
    private final ClasseRepository classeRepository;
    private final InscriptionRepository inscriptionRepository;
    private final ResultatExamenRepository resultatRepository;
    private final RegionService regionService;

    /** Ministère : tout le pays, ou une zone et ses sous-zones. */
    public EvolutionDto nationale(Long zoneId) {
        List<Long> ids = (zoneId == null ? etablissementRepository.findAll() : etablissementRepository.findByZoneEtSousZones(zoneId))
                .stream().map(Etablissement::getId).toList();
        return evolution(ids);
    }

    public EvolutionDto regionale() {
        return evolution(List.copyOf(regionService.etablissementsDeLaRegion().keySet()));
    }

    EvolutionDto evolution(Collection<Long> ids) {
        List<AnneeScolaire> annees = anneeRepository.findAllByOrderByDateDebutDesc().stream()
                .sorted(Comparator.comparing(AnneeScolaire::getDateDebut)).toList();
        Map<Long, long[]> examens = new HashMap<>();
        if (!ids.isEmpty()) {
            for (Object[] l : resultatRepository.totauxParAnnee(ids)) {
                examens.put(((Number) l[0]).longValue(), new long[] {((Number) l[1]).longValue(), ((Number) l[2]).longValue()});
            }
        }
        return new EvolutionDto(annees.stream().map(a -> {
            long[] e = examens.getOrDefault(a.getId(), new long[] {0, 0});
            return new EvolutionDto.PointAnnee(a.getId(), a.getLibelle(),
                    ids.isEmpty() ? 0 : inscriptionRepository.countByEtablissementIdInAndAnneeScolaireIdAndStatut(ids, a.getId(), "ACCEPTEE"),
                    ids.isEmpty() ? 0 : classeRepository.countByEtablissementIdInAndAnneeScolaireId(ids, a.getId()),
                    e[0], e[1], ExamenService.taux(e[1], e[0]));
        }).toList());
    }
}
