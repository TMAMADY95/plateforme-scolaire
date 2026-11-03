package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.AnneeScolaire;
import com.plateforme.scolaire.domain.SituationStatistique;
import com.plateforme.scolaire.domain.SituationStatistiqueNiveau;
import com.plateforme.scolaire.repository.ExamenNationalRepository;
import com.plateforme.scolaire.repository.SituationStatistiqueRepository;
import com.plateforme.scolaire.web.dto.SituationStatistiqueDto;
import com.plateforme.scolaire.web.dto.SituationStatistiqueRequest;
import com.plateforme.scolaire.web.dto.StatistiquesCandidatsDto;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Situation statistique unique, datée et validée : la DGECS fige les chiffres de l'année à un instant donné, d'abord à titre provisoire,
 * puis une seule fois à titre définitif (quand tous les résultats sont proclamés). Les chiffres figés ne changent plus.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class SituationStatistiqueService {

    private final SituationStatistiqueRepository situations;
    private final ExamenNationalRepository examens;
    private final StatistiquesCandidatsService candidats;
    private final StatistiquesExamensService salles;
    private final EntityManager em;
    private final AuditService audit;
    private final Clock clock;

    /** Pur : une situation définitive exige au moins un examen et que tous soient proclamés. */
    static boolean peutEtreDefinitive(List<String> statutsResultats) {
        return !statutsResultats.isEmpty() && statutsResultats.stream().allMatch("PROCLAME"::equals);
    }

    @Transactional(readOnly = true)
    public List<SituationStatistiqueDto> liste(Long anneeId) {
        int inscritsActuels = candidats.statistiques(anneeId).total().inscrits();
        return situations.deLAnnee(anneeId).stream().map(s -> dto(s, inscritsActuels)).toList();
    }

    public SituationStatistiqueDto etablir(Long anneeId, SituationStatistiqueRequest q, String par) {
        String statut = q.statut().trim().toUpperCase();
        if (!"PROVISOIRE".equals(statut) && !"DEFINITIVE".equals(statut)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Statut attendu : PROVISOIRE ou DEFINITIVE");
        }
        AnneeScolaire annee = em.find(AnneeScolaire.class, anneeId);
        if (annee == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Année scolaire introuvable");
        }
        if ("DEFINITIVE".equals(statut)) {
            if (situations.existsByAnneeScolaireIdAndStatut(anneeId, "DEFINITIVE")) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Une situation définitive existe déjà pour cette année");
            }
            List<String> statuts = examens.findByAnneeScolaireIdOrderByLibelle(anneeId).stream().map(x -> x.getStatutResultats()).toList();
            if (!peutEtreDefinitive(statuts)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Une situation définitive exige que les résultats de tous les examens soient proclamés");
            }
        } else if (situations.existsByAnneeScolaireIdAndStatut(anneeId, "DEFINITIVE")) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La situation définitive est établie : plus de situation provisoire pour cette année");
        }
        StatistiquesCandidatsDto c = candidats.statistiques(anneeId);
        SituationStatistique s = new SituationStatistique();
        s.setAnneeScolaire(annee);
        s.setStatut(statut);
        s.setEtablieLe(Instant.now(clock));
        s.setEtabliePar(par);
        s.setCommentaire(q.commentaire() == null || q.commentaire().isBlank() ? null : q.commentaire().trim());
        var t = c.total();
        s.setInscrits(t.inscrits());
        s.setFilles(t.filles());
        s.setLibres(t.libres());
        s.setPresents(t.presents());
        s.setAbsents(t.absents());
        s.setElimines(t.elimines());
        s.setAdmis(t.admis());
        var stats = salles.statistiques(anneeId).salles();
        s.setCentres(stats.centres());
        s.setSalles(stats.sallesUtilisees());
        for (var n : c.parNiveau()) {
            SituationStatistiqueNiveau ligne = new SituationStatistiqueNiveau();
            ligne.setSituation(s);
            ligne.setNiveau(n.libelle());
            ligne.setInscrits(n.inscrits());
            ligne.setFilles(n.filles());
            ligne.setPresents(n.presents());
            ligne.setAdmis(n.admis());
            s.getNiveaux().add(ligne);
        }
        situations.save(s);
        audit.enregistrer("SITUATION_STATISTIQUE_ETABLIE", "AnneeScolaire", anneeId, Map.of("statut", statut, "inscrits", t.inscrits()));
        return dto(s, t.inscrits());
    }

    private static SituationStatistiqueDto dto(SituationStatistique s, int inscritsActuels) {
        return new SituationStatistiqueDto(s.getId(), s.getAnneeScolaire().getId(), s.getStatut(), s.getEtablieLe(), s.getEtabliePar(), s.getCommentaire(),
                s.getInscrits(), s.getFilles(), s.getLibres(), s.getPresents(), s.getAbsents(), s.getElimines(), s.getAdmis(), s.getCentres(), s.getSalles(),
                StatistiquesCandidatsService.pct(s.getAdmis(), s.getPresents()), inscritsActuels - s.getInscrits(),
                s.getNiveaux().stream().map(n -> new SituationStatistiqueDto.Niveau(n.getNiveau(), n.getInscrits(), n.getFilles(), n.getPresents(), n.getAdmis(),
                        StatistiquesCandidatsService.pct(n.getAdmis(), n.getPresents()))).toList());
    }
}
