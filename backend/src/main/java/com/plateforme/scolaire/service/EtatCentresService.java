package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.CaracteristiquesCentre;
import com.plateforme.scolaire.repository.CaracteristiquesCentreRepository;
import com.plateforme.scolaire.repository.CentreExamenRepository;
import com.plateforme.scolaire.web.dto.CaracteristiquesCentreDto;
import com.plateforme.scolaire.web.dto.EtatCentresDto;
import com.plateforme.scolaire.web.dto.EtatCentresDto.LigneCentre;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Caractéristiques des centres d'examen (saisies sur le terrain) et synthèse nationale de leur état et de leurs risques. */
@Service
@RequiredArgsConstructor
@Transactional
public class EtatCentresService {

    private final CaracteristiquesCentreRepository caracteristiques;
    private final CentreExamenRepository centres;
    private final EntityManager em;
    private final AuditService audit;
    private final Clock clock;

    /** Pur : une installation est en mauvais état si l'un des quatre éléments l'est. */
    static boolean installationsMauvaises(String... etats) {
        for (String e : etats) {
            if ("MAUVAIS".equals(e)) {
                return true;
            }
        }
        return false;
    }

    /** Pur : un centre est à risque s'il cumule au moins un risque déclaré ou n'est pas accessible. */
    static boolean aRisque(boolean inondation, boolean enclavement, boolean insecurite, Boolean accessible) {
        return inondation || enclavement || insecurite || Boolean.FALSE.equals(accessible);
    }

    /** Pur : moyenne de candidats par centre, au dixième ; null sans centre. */
    static Double moyenne(int candidats, int centres) {
        return centres <= 0 ? null : BigDecimal.valueOf((double) candidats / centres).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private void verifier(Long examenId, Long centreId) {
        centres.findById(centreId).filter(c -> c.getExamen().getId().equals(examenId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Centre inconnu pour cet examen"));
    }

    @Transactional(readOnly = true)
    public CaracteristiquesCentreDto caracteristiques(Long examenId, Long centreId) {
        verifier(examenId, centreId);
        return caracteristiques.findById(centreId).map(EtatCentresService::dto)
                .orElse(new CaracteristiquesCentreDto(0, null, null, null, null, null, null, null, false, false, false, null));
    }

    public CaracteristiquesCentreDto enregistrer(Long examenId, Long centreId, CaracteristiquesCentreDto q) {
        verifier(examenId, centreId);
        CaracteristiquesCentre c = caracteristiques.findById(centreId).orElseGet(() -> {
            CaracteristiquesCentre n = new CaracteristiquesCentre();
            n.setCentreId(centreId);
            return n;
        });
        c.setSallesReserve(q.sallesReserve());
        c.setEtatTablesBancs(q.etatTablesBancs());
        c.setEtatToilettes(q.etatToilettes());
        c.setEtatEclairage(q.etatEclairage());
        c.setEtatVentilation(q.etatVentilation());
        c.setAccessible(q.accessible());
        c.setDistanceMaxKm(q.distanceMaxKm());
        c.setTempsMaxMin(q.tempsMaxMin());
        c.setRisqueInondation(q.risqueInondation());
        c.setRisqueEnclavement(q.risqueEnclavement());
        c.setRisqueInsecurite(q.risqueInsecurite());
        c.setObservation(q.observation() == null || q.observation().isBlank() ? null : q.observation().trim());
        c.setMisAJourLe(Instant.now(clock));
        caracteristiques.save(c);
        audit.enregistrer("CARACTERISTIQUES_CENTRE_ENREGISTREES", "CentreExamen", centreId, Map.of("sallesReserve", q.sallesReserve()));
        return dto(c);
    }

    private static CaracteristiquesCentreDto dto(CaracteristiquesCentre c) {
        return new CaracteristiquesCentreDto(c.getSallesReserve(), c.getEtatTablesBancs(), c.getEtatToilettes(), c.getEtatEclairage(), c.getEtatVentilation(),
                c.getAccessible(), c.getDistanceMaxKm(), c.getTempsMaxMin(), c.isRisqueInondation(), c.isRisqueEnclavement(), c.isRisqueInsecurite(),
                c.getObservation());
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public EtatCentresDto synthese(Long anneeId) {
        Map<Long, CaracteristiquesCentre> saisies = new HashMap<>();
        for (CaracteristiquesCentre c : (List<CaracteristiquesCentre>) em.createQuery(
                "select k from CaracteristiquesCentre k where k.centreId in (select ce.id from CentreExamen ce where ce.examen.anneeScolaire.id = :a)")
                .setParameter("a", anneeId).getResultList()) {
            saisies.put(c.getCentreId(), c);
        }
        List<LigneCentre> lignes = new ArrayList<>();
        int candidatsTotal = 0;
        for (Object[] r : (List<Object[]>) em.createQuery(
                "select ce.id, ce.nom, x.libelle, z.nom, (select count(k.id) from CandidatExamen k where k.centre = ce) "
                        + ", x.id from CentreExamen ce join ce.examen x left join ce.zone z where x.anneeScolaire.id = :a order by x.libelle, ce.nom")
                .setParameter("a", anneeId).getResultList()) {
            Long id = ((Number) r[0]).longValue();
            int candidats = ((Number) r[4]).intValue();
            candidatsTotal += candidats;
            CaracteristiquesCentre c = saisies.get(id);
            lignes.add(c == null
                    ? new LigneCentre(((Number) r[5]).longValue(), id, (String) r[1], (String) r[2], (String) r[3], candidats, false, 0, null, null, null, null, null, null, null,
                            false, false, false, null)
                    : new LigneCentre(((Number) r[5]).longValue(), id, (String) r[1], (String) r[2], (String) r[3], candidats, true, c.getSallesReserve(), c.getEtatTablesBancs(),
                            c.getEtatToilettes(), c.getEtatEclairage(), c.getEtatVentilation(), c.getAccessible(), c.getDistanceMaxKm(), c.getTempsMaxMin(),
                            c.isRisqueInondation(), c.isRisqueEnclavement(), c.isRisqueInsecurite(), c.getObservation()));
        }
        List<EtatCentresDto.Handicap> handicaps = new ArrayList<>();
        int nbHandicapes = 0;
        for (Object[] r : (List<Object[]>) em.createQuery(
                "select coalesce(n.libelle, 'Niveau non précisé'), c.handicap, count(c.id) from CandidatExamen c join c.examen x left join x.niveau n "
                        + "where x.anneeScolaire.id = :a and c.handicap is not null group by n.libelle, c.handicap order by n.libelle, c.handicap")
                .setParameter("a", anneeId).getResultList()) {
            int nb = ((Number) r[2]).intValue();
            nbHandicapes += nb;
            handicaps.add(new EtatCentresDto.Handicap((String) r[0], (String) r[1], nb));
        }
        int renseignes = (int) lignes.stream().filter(LigneCentre::renseigne).count();
        var resume = new EtatCentresDto.Resume(lignes.size(), renseignes, lignes.stream().mapToInt(LigneCentre::sallesReserve).sum(),
                (int) lignes.stream().filter(l -> installationsMauvaises(l.etatTablesBancs(), l.etatToilettes(), l.etatEclairage(), l.etatVentilation())).count(),
                (int) lignes.stream().filter(l -> Boolean.FALSE.equals(l.accessible())).count(),
                (int) lignes.stream().filter(l -> aRisque(l.risqueInondation(), l.risqueEnclavement(), l.risqueInsecurite(), l.accessible())).count(),
                (int) lignes.stream().filter(LigneCentre::risqueInondation).count(), (int) lignes.stream().filter(LigneCentre::risqueEnclavement).count(),
                (int) lignes.stream().filter(LigneCentre::risqueInsecurite).count(), moyenne(candidatsTotal, lignes.size()), nbHandicapes);
        return new EtatCentresDto(anneeId, resume, lignes, handicaps);
    }
}
