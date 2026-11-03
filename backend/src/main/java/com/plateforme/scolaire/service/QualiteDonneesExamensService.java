package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.ReclamationExamen;
import com.plateforme.scolaire.repository.ReclamationExamenRepository;
import com.plateforme.scolaire.web.dto.QualiteDonneesDto;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Qualité des données des examens (dossiers incomplets, notes manquantes, listes validées) et suivi des réclamations. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QualiteDonneesExamensService {

    private final EntityManager em;
    private final ReclamationExamenRepository reclamations;
    private final Clock clock;

    /** Pur : agrégat des réclamations à l'instant donné. */
    static QualiteDonneesDto.Reclamations agreger(List<ReclamationExamen> liste, Instant maintenant) {
        int ouvertes = 0;
        int acceptees = 0;
        int rejetees = 0;
        int enRetard = 0;
        int dansLeDelai = 0;
        for (ReclamationExamen r : liste) {
            switch (r.getStatut()) {
                case "ACCEPTEE" -> acceptees++;
                case "REJETEE" -> rejetees++;
                default -> ouvertes++;
            }
            if (ReclamationsExamenService.enRetard(r.getStatut(), r.getEcheanceLe(), maintenant)) {
                enRetard++;
            }
            if (ReclamationsExamenService.traiteeDansLeDelai(r)) {
                dansLeDelai++;
            }
        }
        int traitees = acceptees + rejetees;
        return new QualiteDonneesDto.Reclamations(liste.size(), ouvertes, acceptees, rejetees, enRetard,
                StatistiquesCandidatsService.pct(dansLeDelai, traitees), StatistiquesCandidatsService.pct(acceptees, traitees));
    }

    private int compter(String jpql, Long anneeId) {
        return ((Number) em.createQuery(jpql).setParameter("a", anneeId).getSingleResult()).intValue();
    }

    public QualiteDonneesDto qualite(Long anneeId) {
        String base = "select count(c.id) from CandidatExamen c join c.eleve el where c.examen.anneeScolaire.id = :a";
        String identite = " and (el.lieuNaissance is null or el.lieuNaissance = '' or el.dateNaissance is null "
                + "or ((el.nomPere is null or el.nomPere = '') and (el.nomMere is null or el.nomMere = '')))";
        String photo = " and not exists (select 1 from PhotoEleve p where p.eleveId = el.id)";
        String centre = " and c.centre is null";
        int candidats = compter(base, anneeId);
        int sansCentre = compter(base + centre, anneeId);
        int identiteIncomplete = compter(base + identite, anneeId);
        int sansPhoto = compter(base + photo, anneeId);
        int incompletsCumules = compter(base + " and (c.centre is null or " + identite.substring(5) + " or not exists (select 1 from PhotoEleve p where p.eleveId = el.id))", anneeId);
        int presentsSansMoyenne = compter(base + " and c.statutEpreuves = 'PRESENT' and c.moyenne is null", anneeId);
        int presentsSansDecision = compter(base + " and c.statutEpreuves = 'PRESENT' and c.decision is null", anneeId);
        int listes = compter("select count(l.id) from ListeCandidats l where l.examen.anneeScolaire.id = :a", anneeId);
        int validees = compter("select count(l.id) from ListeCandidats l where l.examen.anneeScolaire.id = :a and l.statut = 'VALIDEE'", anneeId);
        var dossiers = new QualiteDonneesDto.Dossiers(candidats, sansCentre, identiteIncomplete, sansPhoto, presentsSansMoyenne, presentsSansDecision,
                listes, validees, StatistiquesCandidatsService.pct(candidats - incompletsCumules, candidats), StatistiquesCandidatsService.pct(validees, listes));
        int rectifies = compter("select count(r.id) from RectificationNote r where r.annuleeLe is null and r.reclamation.examen.anneeScolaire.id = :a", anneeId);
        return new QualiteDonneesDto(anneeId, ReclamationsExamenService.DELAI_JOURS, dossiers,
                agreger(reclamations.deLAnnee(anneeId), Instant.now(clock)), rectifies);
    }
}
