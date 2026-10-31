package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.CandidatExamen;
import com.plateforme.scolaire.domain.CentreExamen;
import com.plateforme.scolaire.domain.IncidentExamen;
import com.plateforme.scolaire.repository.CandidatExamenRepository;
import com.plateforme.scolaire.repository.CentreExamenRepository;
import com.plateforme.scolaire.repository.EpreuveExamenRepository;
import com.plateforme.scolaire.repository.IncidentExamenRepository;
import com.plateforme.scolaire.web.dto.IncidentExamenRequest;
import com.plateforme.scolaire.web.dto.IncidentsCentreDto;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Incidents dans les centres d'examen. Le surveillant qui prend un candidat en flagrant délit de fraude établit un rapport ;
 * le règlement prévoit alors l'élimination du candidat, qui entraîne sa disqualification à l'examen.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class IncidentsExamenService {

    static final Set<String> TYPES = Set.of("FRAUDE", "RETARD", "MALAISE", "PERTURBATION", "FUITE_SUJET", "AUTRE");

    private final IncidentExamenRepository incidents;
    private final CentreExamenRepository centres;
    private final CandidatExamenRepository candidats;
    private final EpreuveExamenRepository epreuves;
    private final AuditService audit;
    private final Clock clock;

    @Transactional(readOnly = true)
    public IncidentsCentreDto incidents(Long examenId, Long centreId) {
        return dto(centre(examenId, centreId));
    }

    public IncidentsCentreDto signaler(Long examenId, Long centreId, IncidentExamenRequest r) {
        CentreExamen centre = centre(examenId, centreId);
        if (!TYPES.contains(r.type())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Type d'incident inconnu : " + r.type());
        }
        CandidatExamen candidat = null;
        if (r.candidatId() != null) {
            candidat = candidats.findById(r.candidatId())
                    .filter(c -> c.getExamen().getId().equals(examenId) && c.getCentre() != null && c.getCentre().getId().equals(centreId))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ce candidat n'est pas affecté à ce centre"));
        }
        var epreuve = r.epreuveId() == null ? null : epreuves.findById(r.epreuveId())
                .filter(e -> e.getExamen().getId().equals(examenId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Épreuve inconnue pour cet examen"));
        if (r.eliminer()) {
            if (!"FRAUDE".equals(r.type()) || candidat == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seule une fraude imputée à un candidat peut entraîner son élimination");
            }
            if ("PROCLAME".equals(centre.getExamen().getStatutResultats())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Les résultats sont proclamés : un candidat ne peut plus être éliminé");
            }
        }
        IncidentExamen i = new IncidentExamen();
        i.setExamen(centre.getExamen());
        i.setCentre(centre);
        i.setCandidat(candidat);
        i.setEpreuve(epreuve);
        i.setType(r.type());
        i.setDescription(r.description().trim());
        i.setSurvenuLe(r.survenuLe() == null ? Instant.now(clock) : r.survenuLe());
        i.setSignalePar(r.signalePar() == null || r.signalePar().isBlank() ? null : r.signalePar().trim());
        i.setElimine(r.eliminer());
        i.setCreatedAt(Instant.now(clock));
        incidents.save(i);
        if (r.eliminer()) {
            candidat.setStatutEpreuves("ELIMINE");
            candidat.getNotes().clear();
            candidat.setMoyenne(null);
            candidat.setDecision("NON_ADMIS");
        }
        audit.enregistrer("INCIDENT_EXAMEN_SIGNALE", "CentreExamen", centreId,
                Map.of("type", r.type(), "elimine", r.eliminer()));
        return dto(centre);
    }

    /** Consigne la décision prise (sanction, suites données) et clôt l'incident. */
    public IncidentsCentreDto traiter(Long examenId, Long centreId, Long incidentId, String decision) {
        CentreExamen centre = centre(examenId, centreId);
        IncidentExamen i = incidents.findById(incidentId).filter(x -> x.getCentre().getId().equals(centreId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Incident introuvable"));
        if (decision == null || decision.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indiquez la décision prise");
        }
        i.setStatut("TRAITE");
        i.setDecision(decision.trim().length() > 1000 ? decision.trim().substring(0, 1000) : decision.trim());
        audit.enregistrer("INCIDENT_EXAMEN_TRAITE", "CentreExamen", centreId, Map.of("incident", incidentId));
        return dto(centre);
    }

    private IncidentsCentreDto dto(CentreExamen centre) {
        List<IncidentExamen> liste = incidents.duCentre(centre.getId());
        Map<String, Long> parType = new LinkedHashMap<>();
        for (String t : List.of("FRAUDE", "RETARD", "MALAISE", "PERTURBATION", "FUITE_SUJET", "AUTRE")) {
            long n = liste.stream().filter(i -> i.getType().equals(t)).count();
            if (n > 0) {
                parType.put(t, n);
            }
        }
        return new IncidentsCentreDto(centre.getId(), centre.getNom(),
                candidats.duCentre(centre.getId()).stream()
                        .map(c -> new IncidentsCentreDto.CandidatCite(c.getId(), (c.getEleve().getPrenom() + " " + c.getEleve().getNom()).trim(), c.getNumeroPv()))
                        .toList(),
                epreuves.findByExamenIdOrderByDateEpreuveAscHeureDebutAsc(centre.getExamen().getId()).stream()
                        .map(e -> new IncidentsCentreDto.EpreuveCitee(e.getId(), e.nom())).toList(),
                liste.stream().map(i -> new IncidentsCentreDto.Incident(i.getId(), i.getType(), i.getDescription(), i.getSurvenuLe(),
                        i.getSignalePar(), i.getStatut(), i.getDecision(), i.isElimine(),
                        i.getCandidat() == null ? null : i.getCandidat().getId(),
                        i.getCandidat() == null ? null : (i.getCandidat().getEleve().getPrenom() + " " + i.getCandidat().getEleve().getNom()).trim(),
                        i.getCandidat() == null ? null : i.getCandidat().getNumeroPv(),
                        i.getEpreuve() == null ? null : i.getEpreuve().getId(), i.getEpreuve() == null ? null : i.getEpreuve().nom())).toList(),
                parType);
    }

    private CentreExamen centre(Long examenId, Long centreId) {
        return centres.findById(centreId).filter(c -> c.getExamen().getId().equals(examenId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Centre inconnu pour cet examen"));
    }
}
