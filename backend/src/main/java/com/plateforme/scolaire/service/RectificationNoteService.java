package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.CandidatExamen;
import com.plateforme.scolaire.domain.DiplomeExamen;
import com.plateforme.scolaire.domain.EpreuveExamen;
import com.plateforme.scolaire.domain.RectificationNote;
import com.plateforme.scolaire.domain.ReclamationExamen;
import com.plateforme.scolaire.domain.ResultatExamen;
import com.plateforme.scolaire.repository.EpreuveExamenRepository;
import com.plateforme.scolaire.repository.RectificationNoteRepository;
import com.plateforme.scolaire.repository.ReclamationExamenRepository;
import com.plateforme.scolaire.repository.ResultatExamenRepository;
import com.plateforme.scolaire.web.dto.ReclamationExamenDto;
import com.plateforme.scolaire.web.dto.RectificationNoteRequest;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Rectification d'une note après proclamation, à la suite d'une réclamation acceptée : la note, la moyenne et la décision du candidat sont
 * recalculées, les résultats consolidés de l'établissement sont ajustés et le diplôme est révoqué si le candidat n'est plus admis.
 * L'ancienne et la nouvelle valeur sont conservées.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class RectificationNoteService {

    private final ReclamationExamenRepository reclamations;
    private final EpreuveExamenRepository epreuves;
    private final ResultatExamenRepository resultats;
    private final RectificationNoteRepository rectifications;
    private final EntityManager em;
    private final AuditService audit;
    private final Clock clock;

    /** Pur : variation du nombre d'admis (+1, -1 ou 0) quand la décision passe d'une valeur à l'autre. */
    static int variationAdmis(String ancienne, String nouvelle) {
        boolean avant = "ADMIS".equals(ancienne);
        boolean apres = "ADMIS".equals(nouvelle);
        return avant == apres ? 0 : (apres ? 1 : -1);
    }

    /** Pur : motif inscrit sur un diplôme révoqué à la suite d'une rectification ; sert aussi à retrouver ce diplôme si la rectification est annulée. */
    static String motifRevocation(Long reclamationId) {
        return "Rectification de note (réclamation n° " + reclamationId + ")";
    }

    public ReclamationExamenDto rectifier(Long examenId, Long reclamationId, RectificationNoteRequest q, String par) {
        ReclamationExamen rec = reclamations.findById(reclamationId).filter(x -> x.getExamen().getId().equals(examenId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Réclamation introuvable"));
        if (!"OUVERTE".equals(rec.getStatut())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette réclamation est déjà traitée");
        }
        if (!"NOTE".equals(rec.getObjet())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seule une réclamation portant sur une note peut entraîner une rectification");
        }
        CandidatExamen c = rec.getCandidat();
        if (!"PRESENT".equals(c.getStatutEpreuves())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Le candidat n'était pas présent : aucune note à rectifier");
        }
        int bareme = rec.getExamen().getBareme();
        if (q.nouvelleNote().compareTo(BigDecimal.valueOf(bareme)) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Les notes sont comprises entre 0 et " + bareme);
        }
        List<EpreuveExamen> liste = epreuves.findByExamenIdOrderByDateEpreuveAscHeureDebutAsc(examenId);
        if (liste.stream().noneMatch(e -> e.getId().equals(q.epreuveId()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Épreuve inconnue pour cet examen");
        }
        EpreuveExamen epreuve = liste.stream().filter(e -> e.getId().equals(q.epreuveId())).findFirst().orElseThrow();
        BigDecimal nouvelle = q.nouvelleNote().setScale(2, RoundingMode.HALF_UP);
        BigDecimal ancienne = c.getNotes().get(q.epreuveId());
        if (ancienne != null && ancienne.compareTo(nouvelle) == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La note est inchangée");
        }
        BigDecimal ancienneMoyenne = c.getMoyenne();
        String ancienneDecision = c.getDecision();

        Map<Long, BigDecimal> notes = new HashMap<>(c.getNotes());
        notes.put(q.epreuveId(), nouvelle);
        Map<Long, Integer> coefficients = new HashMap<>();
        liste.forEach(e -> coefficients.put(e.getId(), e.getCoefficient()));
        BigDecimal moyenne = NotesExamenService.moyenne(coefficients, notes);
        String decision = NotesExamenService.decision(c.getStatutEpreuves(), moyenne, bareme);
        c.getNotes().put(q.epreuveId(), nouvelle);
        c.setMoyenne(moyenne);
        c.setDecision(decision);

        int variation = variationAdmis(ancienneDecision, decision);
        if (variation != 0 && c.getEtablissement() != null) {
            ajusterResultats(examenId, c, variation);
        }
        if (variation < 0) {
            revoquerDiplome(c, rec.getId());
        }
        Instant maintenant = Instant.now(clock);
        RectificationNote r = new RectificationNote();
        r.setReclamation(rec);
        r.setCandidat(c);
        r.setEpreuve(epreuve);
        r.setAncienneNote(ancienne);
        r.setNouvelleNote(nouvelle);
        r.setAncienneMoyenne(ancienneMoyenne);
        r.setNouvelleMoyenne(moyenne);
        r.setAncienneDecision(ancienneDecision);
        r.setNouvelleDecision(decision);
        r.setRectifieeLe(maintenant);
        r.setRectifieePar(par);
        rectifications.save(r);

        rec.setStatut("ACCEPTEE");
        rec.setTraiteeLe(maintenant);
        String d = q.decision().trim();
        rec.setDecision(d.length() > 1000 ? d.substring(0, 1000) : d);
        audit.enregistrer("NOTE_RECTIFIEE", "ExamenNational", examenId, Map.of("reclamation", reclamationId, "candidat", c.getId(),
                "decisionAvant", String.valueOf(ancienneDecision), "decisionApres", String.valueOf(decision)));
        var e = c.getEleve();
        return new ReclamationExamenDto(rec.getId(), c.getId(), c.getNumero(), e.getPrenom() + " " + e.getNom(), rec.getObjet(), rec.getDescription(),
                rec.getStatut(), rec.getDeposeeLe(), rec.getEcheanceLe(), rec.getTraiteeLe(), rec.getDecision(), false);
    }

    /**
     * Annule la rectification la plus récente d'une réclamation : la note, la moyenne et la décision d'origine sont rétablies, les totaux de
     * l'établissement sont recalculés, le diplôme révoqué par cette rectification est rétabli et la réclamation redevient ouverte.
     * Seule la dernière rectification active d'un candidat peut être annulée.
     */
    public ReclamationExamenDto annuler(Long examenId, Long reclamationId, String motif, String par) {
        ReclamationExamen rec = reclamations.findById(reclamationId).filter(x -> x.getExamen().getId().equals(examenId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Réclamation introuvable"));
        RectificationNote r = rectifications.activesDeLaReclamation(reclamationId).stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aucune rectification à annuler pour cette réclamation"));
        CandidatExamen c = r.getCandidat();
        List<RectificationNote> actives = rectifications.activesDuCandidat(c.getId());
        if (actives.isEmpty() || !actives.get(0).getId().equals(r.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Une rectification plus récente existe pour ce candidat : annulez-la d'abord");
        }
        Long epreuveId = r.getEpreuve().getId();
        if (r.getAncienneNote() == null) {
            c.getNotes().remove(epreuveId);
        } else {
            c.getNotes().put(epreuveId, r.getAncienneNote());
        }
        c.setMoyenne(r.getAncienneMoyenne());
        c.setDecision(r.getAncienneDecision());
        int variation = variationAdmis(r.getNouvelleDecision(), r.getAncienneDecision());
        if (variation != 0 && c.getEtablissement() != null) {
            ajusterResultats(examenId, c, variation);
        }
        if (variation > 0) {
            retablirDiplome(c, reclamationId);
        }
        Instant maintenant = Instant.now(clock);
        r.setAnnuleeLe(maintenant);
        r.setAnnuleePar(par);
        String m = motif.trim();
        r.setMotifAnnulation(m.length() > 500 ? m.substring(0, 500) : m);
        rec.setStatut("OUVERTE");
        rec.setTraiteeLe(null);
        rec.setDecision(null);
        audit.enregistrer("RECTIFICATION_ANNULEE", "ExamenNational", examenId, Map.of("reclamation", reclamationId, "candidat", c.getId(),
                "decisionRetablie", String.valueOf(r.getAncienneDecision())));
        var e = c.getEleve();
        return new ReclamationExamenDto(rec.getId(), c.getId(), c.getNumero(), e.getPrenom() + " " + e.getNom(), rec.getObjet(), rec.getDescription(),
                rec.getStatut(), rec.getDeposeeLe(), rec.getEcheanceLe(), rec.getTraiteeLe(), rec.getDecision(), false);
    }

    /** Rétablit uniquement les diplômes révoqués par la rectification annulée (même motif), jamais ceux révoqués pour une autre raison. */
    private void retablirDiplome(CandidatExamen c, Long reclamationId) {
        List<DiplomeExamen> revoques = em.createQuery("select d from DiplomeExamen d where d.candidat.id = :c and d.revoque = true and d.motifRevocation = :m",
                DiplomeExamen.class).setParameter("c", c.getId()).setParameter("m", motifRevocation(reclamationId)).getResultList();
        for (DiplomeExamen d : revoques) {
            d.setRevoque(false);
            d.setRevoqueLe(null);
            d.setMotifRevocation(null);
        }
    }

    /** Ajuste les résultats consolidés de l'établissement (admis, filles admises, série) de ±1. */
    private void ajusterResultats(Long examenId, CandidatExamen c, int variation) {
        ResultatExamen res = resultats.findByExamenIdAndEtablissementId(examenId, c.getEtablissement().getId()).orElse(null);
        if (res == null) {
            return;
        }
        res.setAdmis(Math.max(0, Math.min(res.getPresentes(), res.getAdmis() + variation)));
        if (res.getAdmisFilles() != null && "F".equalsIgnoreCase(c.getEleve().getSexe())) {
            res.setAdmisFilles(Math.max(0, res.getAdmisFilles() + variation));
        }
        if (c.getSerie() != null) {
            res.getSeries().stream().filter(s -> c.getSerie().equals(s.getSerie())).findFirst()
                    .ifPresent(s -> s.setAdmis(Math.max(0, Math.min(s.getPresentes(), s.getAdmis() + variation))));
        }
    }

    private void revoquerDiplome(CandidatExamen c, Long reclamationId) {
        List<DiplomeExamen> valables = em.createQuery("select d from DiplomeExamen d where d.candidat.id = :c and d.revoque = false", DiplomeExamen.class)
                .setParameter("c", c.getId()).getResultList();
        for (DiplomeExamen d : valables) {
            d.setRevoque(true);
            d.setRevoqueLe(Instant.now(clock));
            d.setMotifRevocation(motifRevocation(reclamationId));
        }
    }
}
