package com.plateforme.scolaire.service;

import com.plateforme.scolaire.repository.ExamenNationalRepository;
import com.plateforme.scolaire.web.dto.BudgetExamenDto;
import com.plateforme.scolaire.web.dto.IndicateursDgecsDto;
import com.plateforme.scolaire.web.dto.LogistiqueExamenDto;
import jakarta.persistence.EntityManager;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Indicateurs de performance du directeur général de la DGECS, composés à partir des statistiques, de la qualité, de la logistique et du budget. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IndicateursDgecsService {

    private final EntityManager em;
    private final ExamenNationalRepository examens;
    private final StatistiquesCandidatsService candidats;
    private final QualiteDonneesExamensService qualite;
    private final LogistiqueExamenService logistique;
    private final BudgetExamenService budget;

    /** Pur : délai moyen en jours entre la fin des épreuves et la proclamation ; null sans examen proclamé. */
    static Integer delaiMoyen(List<Long> delais) {
        return delais.isEmpty() ? null : (int) Math.round(delais.stream().mapToLong(Long::longValue).average().orElse(0));
    }

    private long compter(String jpql, Long anneeId) {
        return ((Number) em.createQuery(jpql).setParameter("a", anneeId).getSingleResult()).longValue();
    }

    public IndicateursDgecsDto indicateurs(Long anneeId) {
        var liste = examens.findByAnneeScolaireIdOrderByLibelle(anneeId);
        var stats = candidats.statistiques(anneeId);
        var q = qualite.qualite(anneeId);
        int centres = 0;
        int prets = 0;
        int requis = 0;
        int pourvus = 0;
        long prevu = 0;
        long engage = 0;
        List<Long> delais = new ArrayList<>();
        for (var x : liste) {
            LogistiqueExamenDto l = logistique.suivi(x.getId());
            centres += l.resume().centres();
            prets += l.resume().prets();
            requis += l.resume().postesRequis();
            pourvus += l.resume().postesPourvus();
            BudgetExamenDto b = budget.budget(x.getId());
            prevu += b.total().prevu();
            engage += b.total().engage();
            if (x.getDateFin() != null && x.getDateProclamation() != null) {
                delais.add(ChronoUnit.DAYS.between(x.getDateFin(), x.getDateProclamation()));
            }
        }
        int nbCandidats = stats.total().inscrits();
        long incidents = compter("select count(i.id) from IncidentExamen i where i.examen.anneeScolaire.id = :a", anneeId);
        long admis = compter("select count(c.id) from CandidatExamen c where c.examen.anneeScolaire.id = :a and c.statutEpreuves = 'PRESENT' and c.decision = 'ADMIS'", anneeId);
        long diplomes = compter("select count(d.id) from DiplomeExamen d where d.candidat.examen.anneeScolaire.id = :a and d.revoque = false", anneeId);
        Double incidentsPour10000 = nbCandidats <= 0 ? null : Math.round(incidents * 10000.0 / nbCandidats * 10) / 10.0;
        return new IndicateursDgecsDto(anneeId, liste.size(), nbCandidats, q.dossiers().tauxListesValidees(), q.dossiers().tauxDossiersCompletes(),
                StatistiquesCandidatsService.pct(prets, centres), StatistiquesCandidatsService.pct(pourvus, requis), stats.total().tauxReussite(),
                incidentsPour10000, delaiMoyen(delais), q.reclamations().tauxTraiteesDansLeDelai(), StatistiquesCandidatsService.pct10(diplomes, admis),
                StatistiquesCandidatsService.pct10(engage, prevu), BudgetExamenService.coutMoyen(engage, nbCandidats));
    }
}
