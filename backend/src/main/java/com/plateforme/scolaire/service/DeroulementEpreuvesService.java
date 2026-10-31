package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.CentreExamen;
import com.plateforme.scolaire.domain.EpreuveCentre;
import com.plateforme.scolaire.domain.EpreuveExamen;
import com.plateforme.scolaire.repository.CandidatExamenRepository;
import com.plateforme.scolaire.repository.CentreExamenRepository;
import com.plateforme.scolaire.repository.EpreuveCentreRepository;
import com.plateforme.scolaire.repository.EpreuveExamenRepository;
import com.plateforme.scolaire.web.dto.DeroulementCentreDto;
import com.plateforme.scolaire.web.dto.OuverturePliRequest;
import com.plateforme.scolaire.web.dto.RapportEpreuveRequest;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Déroulement des épreuves dans un centre (règlement général) : les sujets suivent un circuit sécurisé (DGECS, inspection
 * régionale, direction préfectorale ou communale, président de la commission de surveillance). Le pli n'est ouvert qu'au
 * début de l'épreuve, en présence des membres de la commission : aucune autorité ne peut l'ouvrir seule. Après l'épreuve,
 * le secrétariat établit le rapport de surveillance (présents, absents, éliminés, copies ramassées).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class DeroulementEpreuvesService {

    /** Étapes du circuit, dans l'ordre ; l'ouverture est une étape à part, avec ses témoins. */
    static final List<String> CIRCUIT = List.of("A_CONDITIONNER", "CONDITIONNE", "EXPEDIE_IRE", "RECU_DPE", "RECU_PRESIDENT");

    private final EpreuveCentreRepository suivis;
    private final CentreExamenRepository centres;
    private final EpreuveExamenRepository epreuves;
    private final CandidatExamenRepository candidats;
    private final AuditService audit;
    private final Clock clock;

    /** Étape qui suit l'état donné dans le circuit, ou null quand le pli est chez le président (ouverture) ou ouvert. */
    static String etapeSuivante(String statut) {
        int i = CIRCUIT.indexOf(statut);
        return i < 0 || i >= CIRCUIT.size() - 1 ? null : CIRCUIT.get(i + 1);
    }

    /** Écart entre les copies ramassées et celles attendues (présents et éliminés) ; null tant que le rapport est vide. */
    static Integer ecartCopies(Integer presents, Integer elimines, Integer copies) {
        if (presents == null || copies == null) {
            return null;
        }
        return copies - (presents + (elimines == null ? 0 : elimines));
    }

    @Transactional(readOnly = true)
    public DeroulementCentreDto deroulement(Long examenId, Long centreId) {
        return dto(centre(examenId, centreId));
    }

    /** Fait avancer le pli d'une étape du circuit. */
    public DeroulementCentreDto avancer(Long examenId, Long centreId, Long epreuveId) {
        CentreExamen centre = centre(examenId, centreId);
        EpreuveCentre s = suivi(centre, epreuveId);
        String suivante = etapeSuivante(s.getStatutPli());
        if (suivante == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "OUVERT".equals(s.getStatutPli())
                    ? "Le pli est déjà ouvert" : "Le pli est chez le président : il ne peut être ouvert qu'au début de l'épreuve, en présence de deux témoins");
        }
        Instant maintenant = Instant.now(clock);
        s.setStatutPli(suivante);
        switch (suivante) {
            case "CONDITIONNE" -> s.setConditionneLe(maintenant);
            case "EXPEDIE_IRE" -> s.setExpedieLe(maintenant);
            case "RECU_DPE" -> s.setRecuDpeLe(maintenant);
            default -> s.setRecuPresidentLe(maintenant);
        }
        audit.enregistrer("PLI_SUJET_" + suivante, "CentreExamen", centreId, Map.of("epreuve", epreuveId));
        return dto(centre);
    }

    /** Ouverture du pli par le président en présence de deux témoins : trois personnes distinctes. */
    public DeroulementCentreDto ouvrir(Long examenId, Long centreId, Long epreuveId, OuverturePliRequest r) {
        CentreExamen centre = centre(examenId, centreId);
        EpreuveCentre s = suivi(centre, epreuveId);
        if (!"RECU_PRESIDENT".equals(s.getStatutPli())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "OUVERT".equals(s.getStatutPli()) ? "Le pli est déjà ouvert"
                    : "Le pli doit d'abord être reçu par le président de la commission de surveillance");
        }
        String president = r.ouvertPar().trim();
        String t1 = r.temoin1().trim();
        String t2 = r.temoin2().trim();
        if (president.equalsIgnoreCase(t1) || president.equalsIgnoreCase(t2) || t1.equalsIgnoreCase(t2)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le pli s'ouvre en présence de deux témoins : trois personnes différentes sont nécessaires");
        }
        s.setStatutPli("OUVERT");
        s.setOuvertLe(Instant.now(clock));
        s.setOuvertPar(president);
        s.setTemoin1(t1);
        s.setTemoin2(t2);
        audit.enregistrer("PLI_SUJET_OUVERT", "CentreExamen", centreId, Map.of("epreuve", epreuveId, "president", president));
        return dto(centre);
    }

    /** Rapport de surveillance : enregistrable après l'ouverture ; sa clôture exige que tous les candidats soient comptés. */
    public DeroulementCentreDto rapport(Long examenId, Long centreId, Long epreuveId, RapportEpreuveRequest r) {
        CentreExamen centre = centre(examenId, centreId);
        EpreuveCentre s = suivi(centre, epreuveId);
        if (!"OUVERT".equals(s.getStatutPli())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Le rapport se remplit après l'ouverture du pli");
        }
        if (s.isRapportClos()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Le rapport de cette épreuve est clos");
        }
        int candidatsCentre = (int) candidats.countByCentreId(centreId);
        int total = r.presents() + r.absents() + r.elimines();
        if (total > candidatsCentre) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Présents, absents et éliminés (" + total + ") dépassent le nombre de candidats du centre (" + candidatsCentre + ")");
        }
        if (r.copiesRamassees() > r.presents() + r.elimines()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Il y a plus de copies ramassées que de candidats ayant composé");
        }
        if (r.compositionDebut() != null && r.compositionFin() != null && r.compositionFin().isBefore(r.compositionDebut())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'heure de fin précède l'heure de début");
        }
        String observations = r.observations() == null || r.observations().isBlank() ? null : r.observations().trim();
        if (r.cloturer()) {
            if (total != candidatsCentre) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Tous les candidats doivent être comptés avant la clôture : " + total + " sur " + candidatsCentre);
            }
            if (r.copiesRamassees() < r.presents() + r.elimines() && observations == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Il manque des copies : expliquez l'écart dans les observations avant de clore le rapport");
            }
        }
        s.setCompositionDebut(r.compositionDebut());
        s.setCompositionFin(r.compositionFin());
        s.setPresents(r.presents());
        s.setAbsents(r.absents());
        s.setElimines(r.elimines());
        s.setCopiesRamassees(r.copiesRamassees());
        s.setObservations(observations);
        s.setRapportClos(r.cloturer());
        audit.enregistrer(r.cloturer() ? "RAPPORT_EPREUVE_CLOS" : "RAPPORT_EPREUVE_ENREGISTRE", "CentreExamen", centreId, Map.of("epreuve", epreuveId));
        return dto(centre);
    }

    private EpreuveCentre suivi(CentreExamen centre, Long epreuveId) {
        EpreuveExamen epreuve = epreuves.findById(epreuveId).filter(e -> e.getExamen().getId().equals(centre.getExamen().getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Épreuve inconnue pour cet examen"));
        return suivis.findByCentreIdAndEpreuveId(centre.getId(), epreuveId).orElseGet(() -> {
            EpreuveCentre s = new EpreuveCentre();
            s.setCentre(centre);
            s.setEpreuve(epreuve);
            s.setNbExemplaires((int) candidats.countByCentreId(centre.getId()));
            return suivis.save(s);
        });
    }

    private DeroulementCentreDto dto(CentreExamen centre) {
        Map<Long, EpreuveCentre> parEpreuve = suivis.findByCentreId(centre.getId()).stream()
                .collect(Collectors.toMap(s -> s.getEpreuve().getId(), Function.identity()));
        int nbCandidats = (int) candidats.countByCentreId(centre.getId());
        return new DeroulementCentreDto(centre.getId(), centre.getNom(), nbCandidats,
                epreuves.findByExamenIdOrderByDateEpreuveAscHeureDebutAsc(centre.getExamen().getId()).stream().map(e -> {
                    EpreuveCentre s = parEpreuve.get(e.getId());
                    if (s == null) {
                        return new DeroulementCentreDto.Epreuve(e.getId(), e.nom(), e.getDateEpreuve(), e.getHeureDebut(), e.getHeureFin(),
                                nbCandidats, "A_CONDITIONNER", etapeSuivante("A_CONDITIONNER"), null, null, null, null, null, null, null, null,
                                null, null, null, null, null, null, null, false, null);
                    }
                    return new DeroulementCentreDto.Epreuve(e.getId(), e.nom(), e.getDateEpreuve(), e.getHeureDebut(), e.getHeureFin(),
                            s.getNbExemplaires(), s.getStatutPli(), etapeSuivante(s.getStatutPli()), s.getConditionneLe(), s.getExpedieLe(),
                            s.getRecuDpeLe(), s.getRecuPresidentLe(), s.getOuvertLe(), s.getOuvertPar(), s.getTemoin1(), s.getTemoin2(),
                            s.getCompositionDebut(), s.getCompositionFin(), s.getPresents(), s.getAbsents(), s.getElimines(),
                            s.getCopiesRamassees(), s.getObservations(), s.isRapportClos(),
                            ecartCopies(s.getPresents(), s.getElimines(), s.getCopiesRamassees()));
                }).toList());
    }

    private CentreExamen centre(Long examenId, Long centreId) {
        return centres.findById(centreId).filter(c -> c.getExamen().getId().equals(examenId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Centre inconnu pour cet examen"));
    }
}
