package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.BudgetExamen;
import com.plateforme.scolaire.repository.BudgetExamenRepository;
import com.plateforme.scolaire.repository.ExamenNationalRepository;
import com.plateforme.scolaire.web.dto.BudgetExamenDto;
import com.plateforme.scolaire.web.dto.BudgetExamenDto.Ligne;
import com.plateforme.scolaire.web.dto.BudgetExamenRequest;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Budget des examens par rubrique, exécution et coût par candidat et par centre (DGECS). */
@Service
@RequiredArgsConstructor
@Transactional
public class BudgetExamenService {

    static final List<String> RUBRIQUES = List.of("IMPRESSION", "TRANSPORT", "SURVEILLANCE", "CORRECTION", "SECURITE", "SANTE", "COMMUNICATION", "AUTRE");

    private final BudgetExamenRepository budgets;
    private final ExamenNationalRepository examens;
    private final EntityManager em;
    private final AuditService audit;

    /** Pur : coût moyen arrondi au franc ; null sans effectif. */
    static Long coutMoyen(long montant, int effectif) {
        return effectif <= 0 ? null : Math.round((double) montant / effectif);
    }

    /** Pur : somme des lignes (la rubrique du total est « TOTAL »). */
    static Ligne total(List<Ligne> l) {
        return new Ligne("TOTAL", l.stream().mapToLong(Ligne::prevu).sum(), l.stream().mapToLong(Ligne::engage).sum(),
                l.stream().mapToLong(Ligne::paye).sum(), l.stream().mapToLong(Ligne::ecart).sum(), null);
    }

    /** Pur : assemble le budget à partir des lignes saisies (les rubriques non saisies figurent à zéro). */
    static BudgetExamenDto assembler(Long examenId, int candidats, int centres, Map<String, BudgetExamen> saisies) {
        List<Ligne> lignes = new ArrayList<>();
        for (String r : RUBRIQUES) {
            BudgetExamen b = saisies.get(r);
            lignes.add(b == null ? new Ligne(r, 0, 0, 0, 0, null)
                    : new Ligne(r, b.getMontantPrevu(), b.getMontantEngage(), b.getMontantPaye(), b.getMontantEngage() - b.getMontantPrevu(), b.getObservation()));
        }
        Ligne t = total(lignes);
        return new BudgetExamenDto(examenId, candidats, centres, lignes, t, t.prevu() == 0 ? null : StatistiquesCandidatsService.pct10(t.engage(), t.prevu()),
                t.engage() == 0 ? null : StatistiquesCandidatsService.pct10(t.paye(), t.engage()), coutMoyen(t.engage(), candidats), coutMoyen(t.engage(), centres));
    }

    @Transactional(readOnly = true)
    public BudgetExamenDto budget(Long examenId) {
        if (!examens.existsById(examenId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Examen introuvable");
        }
        int candidats = ((Number) em.createQuery("select count(c.id) from CandidatExamen c where c.examen.id = :e").setParameter("e", examenId).getSingleResult()).intValue();
        int centres = ((Number) em.createQuery("select count(c.id) from CentreExamen c where c.examen.id = :e").setParameter("e", examenId).getSingleResult()).intValue();
        Map<String, BudgetExamen> saisies = new java.util.HashMap<>();
        budgets.findByExamenId(examenId).forEach(b -> saisies.put(b.getRubrique(), b));
        return assembler(examenId, candidats, centres, saisies);
    }

    public BudgetExamenDto enregistrer(Long examenId, BudgetExamenRequest q) {
        var examen = examens.findById(examenId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Examen introuvable"));
        String rubrique = q.rubrique().trim().toUpperCase();
        if (!RUBRIQUES.contains(rubrique)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rubrique inconnue : " + q.rubrique());
        }
        if (q.montantPaye() > q.montantEngage()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le montant payé ne peut pas dépasser le montant engagé");
        }
        BudgetExamen b = budgets.findByExamenIdAndRubrique(examenId, rubrique).orElseGet(() -> {
            BudgetExamen n = new BudgetExamen();
            n.setExamen(examen);
            n.setRubrique(rubrique);
            return n;
        });
        b.setMontantPrevu(q.montantPrevu());
        b.setMontantEngage(q.montantEngage());
        b.setMontantPaye(q.montantPaye());
        b.setObservation(q.observation() == null || q.observation().isBlank() ? null : q.observation().trim());
        budgets.save(b);
        audit.enregistrer("BUDGET_EXAMEN_ENREGISTRE", "ExamenNational", examenId, Map.of("rubrique", rubrique, "engage", q.montantEngage()));
        return budget(examenId);
    }
}
