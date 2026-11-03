package com.plateforme.scolaire.service;

import com.plateforme.scolaire.web.dto.ParticipationDto;
import com.plateforme.scolaire.web.dto.ParticipationDto.Centre;
import com.plateforme.scolaire.web.dto.ParticipationDto.Epreuve;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Suivi national de la participation, épreuve par épreuve puis centre par centre, à partir des rapports de surveillance déjà saisis
 * dans les centres. Les attendus d'un centre sont ses candidats affectés ; seuls les centres ayant remis un rapport comptent dans les taux.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ParticipationExamensService {

    /** Tolérance (minutes) pour considérer qu'une épreuve a commencé ou fini à l'heure. */
    static final int TOLERANCE_MINUTES = 15;
    static final String ZONE_HORAIRE = "Africa/Conakry";

    private final EntityManager em;

    private static int entier(Object o) {
        return o == null ? 0 : ((Number) o).intValue();
    }

    private static Integer entierOuNull(Object o) {
        return o == null ? null : ((Number) o).intValue();
    }

    /** Pur : pourcentage de présents parmi les attendus ; null sans attendu. */
    static Double tauxPresence(int presents, int attendus) {
        return StatistiquesCandidatsService.pct(presents, attendus);
    }

    /** Pur : cumul des épreuves. */
    static ParticipationDto.Resume resume(List<Epreuve> l, int rapportsAttendus) {
        int attendus = l.stream().mapToInt(Epreuve::attendus).sum();
        int presents = l.stream().mapToInt(Epreuve::presents).sum();
        int absents = l.stream().mapToInt(Epreuve::absents).sum();
        int debutsOk = l.stream().mapToInt(Epreuve::debutsALHeure).sum();
        int debutsTot = l.stream().mapToInt(Epreuve::debutsRenseignes).sum();
        int finsOk = l.stream().mapToInt(Epreuve::finsALHeure).sum();
        int finsTot = l.stream().mapToInt(Epreuve::finsRenseignees).sum();
        return new ParticipationDto.Resume(l.size(), rapportsAttendus, l.stream().mapToInt(Epreuve::rapportsRecus).sum(),
                l.stream().mapToInt(Epreuve::rapportsClos).sum(), attendus, presents, absents, l.stream().mapToInt(Epreuve::elimines).sum(),
                tauxPresence(presents, attendus), StatistiquesCandidatsService.pct(absents, attendus), StatistiquesCandidatsService.pct(debutsOk, debutsTot),
                StatistiquesCandidatsService.pct(finsOk, finsTot));
    }

    private static final String PREVU_DEBUT = "((e.date_epreuve + e.heure_debut) AT TIME ZONE '" + ZONE_HORAIRE + "') + interval '" + TOLERANCE_MINUTES + " minutes'";
    private static final String PREVU_FIN = "((e.date_epreuve + e.heure_fin) AT TIME ZONE '" + ZONE_HORAIRE + "') + interval '" + TOLERANCE_MINUTES + " minutes'";

    @SuppressWarnings("unchecked")
    public ParticipationDto participation(Long anneeId) {
        List<Object[]> rows = em.createNativeQuery(
                "SELECT e.id, x.libelle, coalesce(m.libelle, e.libelle, 'Épreuve'), e.date_epreuve::text, e.heure_debut::text, e.heure_fin::text, "
                        + "(SELECT count(*) FROM centre_examen c WHERE c.examen_id = x.id), "
                        + "count(ec.id), count(ec.id) FILTER (WHERE ec.rapport_clos), "
                        + "coalesce(sum(cc.n) FILTER (WHERE ec.presents IS NOT NULL), 0), coalesce(sum(ec.presents), 0), coalesce(sum(ec.absents), 0), "
                        + "coalesce(sum(ec.elimines), 0), coalesce(sum(ec.copies_ramassees), 0), "
                        + "count(*) FILTER (WHERE ec.composition_debut IS NOT NULL AND ec.composition_debut <= " + PREVU_DEBUT + "), "
                        + "count(*) FILTER (WHERE ec.composition_debut IS NOT NULL), "
                        + "count(*) FILTER (WHERE ec.composition_fin IS NOT NULL AND ec.composition_fin <= " + PREVU_FIN + "), "
                        + "count(*) FILTER (WHERE ec.composition_fin IS NOT NULL) "
                        + "FROM epreuve_examen e JOIN examen_national x ON x.id = e.examen_id LEFT JOIN matiere m ON m.id = e.matiere_id "
                        + "LEFT JOIN epreuve_centre ec ON ec.epreuve_id = e.id "
                        + "LEFT JOIN (SELECT centre_id, count(*) AS n FROM candidat_examen WHERE centre_id IS NOT NULL GROUP BY centre_id) cc ON cc.centre_id = ec.centre_id "
                        + "WHERE x.annee_scolaire_id = :a GROUP BY e.id, x.libelle, m.libelle, x.id ORDER BY e.date_epreuve, e.heure_debut, x.libelle")
                .setParameter("a", anneeId).getResultList();
        List<Epreuve> epreuves = new ArrayList<>();
        int rapportsAttendus = 0;
        for (Object[] r : rows) {
            int attendus = entier(r[9]);
            int presents = entier(r[10]);
            int absents = entier(r[11]);
            rapportsAttendus += entier(r[6]);
            epreuves.add(new Epreuve(((Number) r[0]).longValue(), (String) r[1], (String) r[2], (String) r[3], (String) r[4], (String) r[5], entier(r[6]),
                    entier(r[7]), entier(r[8]), attendus, presents, absents, entier(r[12]), entier(r[13]), tauxPresence(presents, attendus),
                    StatistiquesCandidatsService.pct(absents, attendus), entier(r[14]), entier(r[15]), entier(r[16]), entier(r[17])));
        }
        return new ParticipationDto(anneeId, resume(epreuves, rapportsAttendus), epreuves);
    }

    /** Centres d'une épreuve, ceux sans rapport inclus ; les plus fortes absences d'abord. */
    @SuppressWarnings("unchecked")
    public List<Centre> centres(Long epreuveId) {
        Number existe = (Number) em.createNativeQuery("SELECT count(*) FROM epreuve_examen WHERE id = :e").setParameter("e", epreuveId).getSingleResult();
        if (existe.intValue() == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Épreuve introuvable");
        }
        List<Object[]> rows = em.createNativeQuery(
                "SELECT c.id, c.nom, z.nom, ec.id IS NOT NULL, coalesce(ec.rapport_clos, false), "
                        + "(SELECT count(*) FROM candidat_examen k WHERE k.centre_id = c.id), ec.presents, ec.absents, ec.elimines, ec.copies_ramassees, "
                        + "to_char(ec.composition_debut AT TIME ZONE '" + ZONE_HORAIRE + "', 'HH24:MI'), "
                        + "CASE WHEN ec.composition_debut IS NULL THEN NULL ELSE ec.composition_debut <= " + PREVU_DEBUT + " END, "
                        + "CASE WHEN ec.composition_fin IS NULL THEN NULL ELSE ec.composition_fin <= " + PREVU_FIN + " END "
                        + "FROM epreuve_examen e JOIN centre_examen c ON c.examen_id = e.examen_id LEFT JOIN zone_geographique z ON z.id = c.zone_id "
                        + "LEFT JOIN epreuve_centre ec ON ec.epreuve_id = e.id AND ec.centre_id = c.id WHERE e.id = :e")
                .setParameter("e", epreuveId).getResultList();
        List<Centre> res = new ArrayList<>();
        for (Object[] r : rows) {
            int attendus = entier(r[5]);
            Integer presents = entierOuNull(r[6]);
            Integer absents = entierOuNull(r[7]);
            res.add(new Centre(((Number) r[0]).longValue(), (String) r[1], (String) r[2], Boolean.TRUE.equals(r[3]), Boolean.TRUE.equals(r[4]), attendus,
                    presents, absents, entierOuNull(r[8]), entierOuNull(r[9]), presents == null ? null : tauxPresence(presents, attendus),
                    absents == null ? null : StatistiquesCandidatsService.pct(absents, attendus), (String) r[10], (Boolean) r[11], (Boolean) r[12]));
        }
        res.sort(Comparator.comparing((Centre c) -> c.tauxAbsence() == null ? -1.0 : c.tauxAbsence()).reversed().thenComparing(Centre::centre));
        return res;
    }
}
