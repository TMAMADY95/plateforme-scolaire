package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.ExamenNational;
import com.plateforme.scolaire.domain.ReclamationExamen;
import com.plateforme.scolaire.repository.ExamenNationalRepository;
import com.plateforme.scolaire.repository.ReclamationExamenRepository;
import com.plateforme.scolaire.web.dto.ReclamationExamenDto;
import com.plateforme.scolaire.web.dto.ReclamationExamenRequest;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Dépôt et traitement des réclamations sur les résultats d'un examen (DGECS). */
@Service
@RequiredArgsConstructor
@Transactional
public class ReclamationsExamenService {

    public static final int DELAI_JOURS = 15;
    static final Set<String> OBJETS = Set.of("NOTE", "IDENTITE", "OMISSION", "AUTRE");

    private final ReclamationExamenRepository reclamations;
    private final ExamenNationalRepository examens;
    private final EntityManager em;
    private final AuditService audit;
    private final Clock clock;

    /** Pur : échéance de traitement d'une réclamation déposée à l'instant donné. */
    static Instant echeance(Instant depot) {
        return depot.plus(Duration.ofDays(DELAI_JOURS));
    }

    /** Pur : une réclamation est en retard si elle est encore ouverte après l'échéance. */
    static boolean enRetard(String statut, Instant echeance, Instant maintenant) {
        return "OUVERTE".equals(statut) && maintenant.isAfter(echeance);
    }

    /** Pur : traitée dans le délai si elle est close et que la clôture précède l'échéance. */
    static boolean traiteeDansLeDelai(ReclamationExamen r) {
        return r.getTraiteeLe() != null && !r.getTraiteeLe().isAfter(r.getEcheanceLe());
    }

    @Transactional(readOnly = true)
    public List<ReclamationExamenDto> liste(Long examenId) {
        examen(examenId);
        Instant maintenant = Instant.now(clock);
        return reclamations.deLExamen(examenId).stream().map(r -> dto(r, maintenant)).toList();
    }

    public ReclamationExamenDto deposer(Long examenId, ReclamationExamenRequest q) {
        ExamenNational examen = examen(examenId);
        if (!"PROCLAME".equals(examen.getStatutResultats())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Les résultats ne sont pas encore proclamés : aucune réclamation possible");
        }
        String objet = q.objet().trim().toUpperCase();
        if (!OBJETS.contains(objet)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Objet de réclamation inconnu : " + q.objet());
        }
        var candidats = em.createQuery("select c from CandidatExamen c join fetch c.eleve where c.examen.id = :e and c.numero = :n",
                com.plateforme.scolaire.domain.CandidatExamen.class).setParameter("e", examenId).setParameter("n", q.numeroCandidat().trim()).getResultList();
        if (candidats.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucun candidat de cet examen n'a ce numéro");
        }
        Instant maintenant = Instant.now(clock);
        ReclamationExamen r = new ReclamationExamen();
        r.setExamen(examen);
        r.setCandidat(candidats.get(0));
        r.setObjet(objet);
        r.setDescription(q.description().trim());
        r.setDeposeeLe(maintenant);
        r.setEcheanceLe(echeance(maintenant));
        reclamations.save(r);
        audit.enregistrer("RECLAMATION_EXAMEN_DEPOSEE", "ExamenNational", examenId, Map.of("reclamation", r.getId(), "objet", objet));
        return dto(r, maintenant);
    }

    public ReclamationExamenDto traiter(Long examenId, Long id, String statut, String decision) {
        if (!"ACCEPTEE".equals(statut) && !"REJETEE".equals(statut)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Décision attendue : ACCEPTEE ou REJETEE");
        }
        if (decision == null || decision.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indiquez la motivation de la décision");
        }
        ReclamationExamen r = reclamations.findById(id).filter(x -> x.getExamen().getId().equals(examenId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Réclamation introuvable"));
        if (!"OUVERTE".equals(r.getStatut())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette réclamation est déjà traitée");
        }
        Instant maintenant = Instant.now(clock);
        r.setStatut(statut);
        r.setTraiteeLe(maintenant);
        String d = decision.trim();
        r.setDecision(d.length() > 1000 ? d.substring(0, 1000) : d);
        audit.enregistrer("RECLAMATION_EXAMEN_TRAITEE", "ExamenNational", examenId, Map.of("reclamation", id, "statut", statut));
        return dto(r, maintenant);
    }

    private ExamenNational examen(Long id) {
        return examens.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Examen introuvable"));
    }

    private static ReclamationExamenDto dto(ReclamationExamen r, Instant maintenant) {
        var e = r.getCandidat().getEleve();
        return new ReclamationExamenDto(r.getId(), r.getCandidat().getId(), r.getCandidat().getNumero(), e.getPrenom() + " " + e.getNom(), r.getObjet(),
                r.getDescription(), r.getStatut(), r.getDeposeeLe(), r.getEcheanceLe(), r.getTraiteeLe(), r.getDecision(),
                enRetard(r.getStatut(), r.getEcheanceLe(), maintenant));
    }
}
