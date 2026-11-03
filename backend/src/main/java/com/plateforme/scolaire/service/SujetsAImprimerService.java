package com.plateforme.scolaire.service;

import com.plateforme.scolaire.web.dto.SujetsAImprimerDto;
import com.plateforme.scolaire.web.dto.SujetsAImprimerDto.Epreuve;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Calcule les besoins d'impression d'un examen : un sujet par candidat plus une marge, des feuilles de composition par candidat et une enveloppe
 * (pli) par salle. La marge et le nombre de feuilles sont des hypothèses de travail à confirmer avec la DGECS.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SujetsAImprimerService {

    static final int MARGE_POURCENT = 5;
    static final int FEUILLES_PAR_CANDIDAT = 2;

    private final EntityManager em;

    /** Pur : sujets à imprimer = candidats + marge (arrondie à l'entier supérieur) ; 0 sans candidat. */
    static int sujets(int candidats) {
        return candidats <= 0 ? 0 : candidats + (int) Math.ceil(candidats * MARGE_POURCENT / 100.0);
    }

    /** Pur : écart entre les exemplaires conditionnés et ceux à imprimer (négatif : il en manque). */
    static int ecart(int conditionnes, int aImprimer) {
        return conditionnes - aImprimer;
    }

    @SuppressWarnings("unchecked")
    public SujetsAImprimerDto besoins(Long examenId) {
        if (em.find(com.plateforme.scolaire.domain.ExamenNational.class, examenId) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Examen introuvable");
        }
        int candidats = ((Number) em.createQuery("select count(c.id) from CandidatExamen c where c.examen.id = :e").setParameter("e", examenId).getSingleResult()).intValue();
        int salles = 0;
        for (Object[] r : (List<Object[]>) em.createQuery("select c.centre.id, max(c.salle) from CandidatExamen c where c.examen.id = :e and c.centre is not null "
                + "group by c.centre.id").setParameter("e", examenId).getResultList()) {
            salles += r[1] == null ? 0 : ((Number) r[1]).intValue();
        }
        List<Epreuve> epreuves = new ArrayList<>();
        for (Object[] r : (List<Object[]>) em.createNativeQuery(
                "SELECT e.id, coalesce(m.libelle, e.libelle, 'Épreuve'), e.date_epreuve::text, coalesce(sum(ec.nb_exemplaires), 0) "
                        + "FROM epreuve_examen e LEFT JOIN matiere m ON m.id = e.matiere_id LEFT JOIN epreuve_centre ec ON ec.epreuve_id = e.id "
                        + "WHERE e.examen_id = :e GROUP BY e.id, m.libelle, e.libelle, e.date_epreuve, e.heure_debut ORDER BY e.date_epreuve, e.heure_debut")
                .setParameter("e", examenId).getResultList()) {
            int aImprimer = sujets(candidats);
            int conditionnes = ((Number) r[3]).intValue();
            epreuves.add(new Epreuve(((Number) r[0]).longValue(), (String) r[1], (String) r[2], candidats, aImprimer, candidats * FEUILLES_PAR_CANDIDAT, salles,
                    conditionnes, ecart(conditionnes, aImprimer)));
        }
        List<SujetsAImprimerDto.Option> options = new ArrayList<>();
        for (Object[] r : (List<Object[]>) em.createQuery("select coalesce(c.serie, 'Sans série'), count(c.id) from CandidatExamen c where c.examen.id = :e "
                + "group by c.serie order by c.serie").setParameter("e", examenId).getResultList()) {
            String serie = (String) r[0];
            int n = ((Number) r[1]).intValue();
            options.add(new SujetsAImprimerDto.Option(StatistiquesExamensService.libelleSerie("Sans série".equals(serie) ? null : serie).replace("Série non précisée", "Sans série"),
                    n, sujets(n)));
        }
        return new SujetsAImprimerDto(examenId, candidats, salles, MARGE_POURCENT, FEUILLES_PAR_CANDIDAT, epreuves, options);
    }
}
