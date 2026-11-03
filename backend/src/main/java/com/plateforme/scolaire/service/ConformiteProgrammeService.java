package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.Classe;
import com.plateforme.scolaire.domain.Creneau;
import com.plateforme.scolaire.domain.Matiere;
import com.plateforme.scolaire.domain.ProgrammeMatiere;
import com.plateforme.scolaire.repository.ClasseRepository;
import com.plateforme.scolaire.repository.CreneauRepository;
import com.plateforme.scolaire.repository.ProgrammeMatiereRepository;
import com.plateforme.scolaire.web.dto.ConformiteProgrammeDto;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Contrôle indicatif (non bloquant) : l'emploi du temps d'une classe respecte-t-il les heures du programme officiel ? */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConformiteProgrammeService {

    private final ClasseRepository classes;
    private final CreneauRepository creneaux;
    private final ProgrammeMatiereRepository programmes;

    /** Pur : heures décimales (minutes / 60, deux décimales). */
    static BigDecimal heures(long minutes) {
        return BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    /** Pur : une ligne par matière présente au programme ou dans l'emploi du temps. */
    static List<ConformiteProgrammeDto.Ligne> comparer(Map<Long, ProgrammeMatiere> prevus, Map<Long, Matiere> matieres, Map<Long, Long> minutesPlanifiees) {
        Map<Long, ConformiteProgrammeDto.Ligne> lignes = new LinkedHashMap<>();
        for (ProgrammeMatiere p : prevus.values()) {
            BigDecimal prevues = p.getHeuresHebdo();
            BigDecimal planifiees = heures(minutesPlanifiees.getOrDefault(p.getMatiere().getId(), 0L));
            String statut;
            BigDecimal ecart = null;
            if (prevues == null) {
                statut = "A_PRECISER";
            } else {
                ecart = planifiees.subtract(prevues);
                statut = planifiees.signum() == 0 && prevues.signum() > 0 ? "NON_PLANIFIEE"
                        : ecart.signum() == 0 ? "CONFORME" : ecart.signum() < 0 ? "MANQUE" : "EXCES";
            }
            lignes.put(p.getMatiere().getId(), new ConformiteProgrammeDto.Ligne(p.getMatiere().getId(), p.getMatiere().getLibelle(), prevues, planifiees, ecart, statut));
        }
        minutesPlanifiees.forEach((id, minutes) -> {
            if (!lignes.containsKey(id)) {
                BigDecimal h = heures(minutes);
                lignes.put(id, new ConformiteProgrammeDto.Ligne(id, matieres.get(id).getLibelle(), null, h, null, "HORS_PROGRAMME"));
            }
        });
        return lignes.values().stream().sorted(Comparator.comparing(ConformiteProgrammeDto.Ligne::matiere)).toList();
    }

    public ConformiteProgrammeDto controler(Long classeId) {
        Classe classe = classes.findById(classeId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Classe introuvable"));
        Map<Long, ProgrammeMatiere> prevus = new LinkedHashMap<>();
        for (ProgrammeMatiere p : programmes.pourClasse(classe.getAnneeScolaire().getId(), classe.getNiveau().getId(), classe.getSerie())) {
            ProgrammeMatiere deja = prevus.get(p.getMatiere().getId());
            if (deja == null || (deja.getSerie() == null && p.getSerie() != null)) {
                prevus.put(p.getMatiere().getId(), p);
            }
        }
        Map<Long, Long> minutes = new LinkedHashMap<>();
        Map<Long, Matiere> matieres = new LinkedHashMap<>();
        for (Creneau c : creneaux.findByClasseIdOrderByJourSemaineAscHeureDebutAsc(classeId)) {
            Long id = c.getMatiere().getId();
            matieres.put(id, c.getMatiere());
            minutes.merge(id, Duration.between(c.getHeureDebut(), c.getHeureFin()).toMinutes(), Long::sum);
        }
        List<ConformiteProgrammeDto.Ligne> lignes = comparer(prevus, matieres, minutes);
        BigDecimal totalPrevu = prevus.values().stream().map(ProgrammeMatiere::getHeuresHebdo).filter(h -> h != null).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPlanifie = heures(minutes.values().stream().mapToLong(Long::longValue).sum());
        return new ConformiteProgrammeDto(classeId, !prevus.isEmpty(), totalPrevu, totalPlanifie, lignes);
    }
}
