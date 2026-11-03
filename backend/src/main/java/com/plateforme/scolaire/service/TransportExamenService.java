package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.ConvoiExamen;
import com.plateforme.scolaire.repository.CentreExamenRepository;
import com.plateforme.scolaire.repository.ConvoiExamenRepository;
import com.plateforme.scolaire.web.dto.ConvoiExamenDto;
import com.plateforme.scolaire.web.dto.ConvoiExamenDto.Convoi;
import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Suivi du transport des sujets et du matériel : véhicules, itinéraires, horaires, carburant, incidents, centres desservis. */
@Service
@RequiredArgsConstructor
@Transactional
public class TransportExamenService {

    static final Set<String> STATUTS = Set.of("PREVU", "EN_ROUTE", "LIVRE");
    static final Set<String> INCIDENTS = Set.of("PERTE", "RETARD", "ERREUR_DISTRIBUTION", "RUPTURE_MATERIEL", "ACCIDENT", "AUTRE");

    private final ConvoiExamenRepository convois;
    private final CentreExamenRepository centres;
    private final AuditService audit;
    private final Clock clock;

    /** Pur : un convoi livré est en retard s'il est arrivé après l'heure prévue ; un convoi non livré l'est dès que l'heure prévue est dépassée. */
    static boolean enRetard(String statut, Instant arriveePrevue, Instant arriveeReelle, Instant maintenant) {
        if (arriveePrevue == null) {
            return false;
        }
        if ("LIVRE".equals(statut)) {
            return arriveeReelle != null && arriveeReelle.isAfter(arriveePrevue);
        }
        return maintenant.isAfter(arriveePrevue);
    }

    /** Pur : résumé du transport. */
    static ConvoiExamenDto.Resume resume(int nbCentres, List<Convoi> l) {
        Set<Long> desservis = new HashSet<>();
        Set<String> vehicules = new HashSet<>();
        int livres = 0;
        int enRoute = 0;
        int retard = 0;
        int incidents = 0;
        int carburant = 0;
        int escortes = 0;
        int livresATemps = 0;
        for (Convoi c : l) {
            desservis.add(c.centreId());
            vehicules.add(c.vehicule().trim().toLowerCase());
            carburant += c.carburantLitres() == null ? 0 : c.carburantLitres();
            escortes += c.escorte() ? 1 : 0;
            incidents += c.incidentType() != null ? 1 : 0;
            retard += c.enRetard() ? 1 : 0;
            if ("LIVRE".equals(c.statut())) {
                livres++;
                livresATemps += c.enRetard() ? 0 : 1;
            } else if ("EN_ROUTE".equals(c.statut())) {
                enRoute++;
            }
        }
        return new ConvoiExamenDto.Resume(nbCentres, desservis.size(), Math.max(0, nbCentres - desservis.size()), l.size(), livres, enRoute, retard, incidents,
                vehicules.size(), carburant, escortes, StatistiquesCandidatsService.pct(livresATemps, livres));
    }

    @Transactional(readOnly = true)
    public ConvoiExamenDto suivi(Long examenId) {
        Instant maintenant = Instant.now(clock);
        int nbCentres = centres.deLExamen(examenId).size();
        List<Convoi> liste = convois.deLExamen(examenId).stream().map(c -> dto(c, maintenant)).toList();
        return new ConvoiExamenDto(examenId, resume(nbCentres, liste), liste);
    }

    public ConvoiExamenDto creer(Long examenId, ConvoiExamenDto.Requete q) {
        ConvoiExamen c = new ConvoiExamen();
        appliquer(c, examenId, q);
        convois.save(c);
        audit.enregistrer("CONVOI_EXAMEN_CREE", "ExamenNational", examenId, Map.of("centre", q.centreId(), "vehicule", q.vehicule().trim()));
        return suivi(examenId);
    }

    public ConvoiExamenDto modifier(Long examenId, Long id, ConvoiExamenDto.Requete q) {
        ConvoiExamen c = convois.findById(id).filter(x -> x.getExamen().getId().equals(examenId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Convoi introuvable"));
        appliquer(c, examenId, q);
        audit.enregistrer("CONVOI_EXAMEN_MODIFIE", "ExamenNational", examenId, Map.of("convoi", id, "statut", c.getStatut()));
        return suivi(examenId);
    }

    public ConvoiExamenDto supprimer(Long examenId, Long id) {
        ConvoiExamen c = convois.findById(id).filter(x -> x.getExamen().getId().equals(examenId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Convoi introuvable"));
        convois.delete(c);
        audit.enregistrer("CONVOI_EXAMEN_SUPPRIME", "ExamenNational", examenId, Map.of("convoi", id));
        return suivi(examenId);
    }

    private void appliquer(ConvoiExamen c, Long examenId, ConvoiExamenDto.Requete q) {
        var centre = centres.findById(q.centreId()).filter(x -> x.getExamen().getId().equals(examenId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Centre inconnu pour cet examen"));
        String statut = q.statut().trim().toUpperCase();
        if (!STATUTS.contains(statut)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Statut inconnu : " + q.statut());
        }
        String incident = q.incidentType() == null || q.incidentType().isBlank() ? null : q.incidentType().trim().toUpperCase();
        if (incident != null && !INCIDENTS.contains(incident)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Type d'incident inconnu : " + q.incidentType());
        }
        if (q.departPrevu() != null && q.arriveePrevue() != null && q.arriveePrevue().isBefore(q.departPrevu())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'arrivée prévue précède le départ prévu");
        }
        Instant arrivee = q.arriveeReelle();
        if ("LIVRE".equals(statut) && arrivee == null) {
            arrivee = Instant.now(clock);
        }
        if (!"LIVRE".equals(statut)) {
            arrivee = null;
        }
        c.setExamen(centre.getExamen());
        c.setCentre(centre);
        c.setVehicule(q.vehicule().trim());
        c.setChauffeur(vide(q.chauffeur()));
        c.setItineraire(vide(q.itineraire()));
        c.setEscorte(q.escorte());
        c.setDepartPrevu(q.departPrevu());
        c.setArriveePrevue(q.arriveePrevue());
        c.setDepartReel(q.departReel());
        c.setArriveeReelle(arrivee);
        c.setCarburantLitres(q.carburantLitres());
        c.setStatut(statut);
        c.setIncidentType(incident);
        c.setIncidentDescription(incident == null ? null : vide(q.incidentDescription()));
        c.setObservation(vide(q.observation()));
    }

    private static String vide(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static Convoi dto(ConvoiExamen c, Instant maintenant) {
        return new Convoi(c.getId(), c.getCentre().getId(), c.getCentre().getNom(), c.getVehicule(), c.getChauffeur(), c.getItineraire(), c.isEscorte(),
                c.getDepartPrevu(), c.getArriveePrevue(), c.getDepartReel(), c.getArriveeReelle(), c.getCarburantLitres(), c.getStatut(), c.getIncidentType(),
                c.getIncidentDescription(), c.getObservation(), enRetard(c.getStatut(), c.getArriveePrevue(), c.getArriveeReelle(), maintenant));
    }
}
