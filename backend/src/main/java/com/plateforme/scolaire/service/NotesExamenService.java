package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.CandidatExamen;
import com.plateforme.scolaire.domain.EpreuveExamen;
import com.plateforme.scolaire.domain.ExamenNational;
import com.plateforme.scolaire.repository.CandidatExamenRepository;
import com.plateforme.scolaire.repository.EpreuveExamenRepository;
import com.plateforme.scolaire.repository.ExamenNationalRepository;
import com.plateforme.scolaire.web.dto.NotesCandidatsDto;
import com.plateforme.scolaire.web.dto.ResultatExamenRequest;
import com.plateforme.scolaire.web.dto.SaisieNotesRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Résultats individuels : statut de chaque candidat (présent, absent, éliminé pour fraude), notes par épreuve, moyenne
 * pondérée par les coefficients et décision proposée (admis à partir de 10/20, la délibération du jury restant souveraine).
 * Les chiffres d'un établissement se déduisent ensuite des candidats.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class NotesExamenService {

    static final Set<String> STATUTS = Set.of("INSCRIT", "PRESENT", "ABSENT", "ELIMINE");
    static final BigDecimal MOYENNE_ADMISSION = BigDecimal.TEN;
    static final BigDecimal NOTE_MAX = BigDecimal.valueOf(20);

    private final CandidatExamenRepository candidats;
    private final EpreuveExamenRepository epreuves;
    private final ExamenNationalRepository examens;
    private final ExamenService examenService;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public NotesCandidatsDto notes(Long examenId, Long etablissementId) {
        ExamenNational examen = examen(examenId);
        List<EpreuveExamen> liste = epreuves.findByExamenIdOrderByDateEpreuveAscHeureDebutAsc(examenId);
        return new NotesCandidatsDto(examenId, examen.getStatutResultats(),
                liste.stream().map(e -> new NotesCandidatsDto.Epreuve(e.getId(), e.nom(), e.getCoefficient())).toList(),
                candidats.deLEtablissement(examenId, etablissementId).stream()
                        .map(c -> new NotesCandidatsDto.Candidat(c.getId(), (c.getEleve().getPrenom() + " " + c.getEleve().getNom()).trim(),
                                c.getEleve().getSexe(), c.getNumeroPv(), c.getStatutEpreuves(), new HashMap<>(c.getNotes()),
                                c.getMoyenne(), c.getDecision()))
                        .toList());
    }

    public NotesCandidatsDto.Candidat saisir(Long examenId, Long etablissementId, Long candidatId, SaisieNotesRequest r) {
        ExamenNational examen = examen(examenId);
        verifierOuvert(examen);
        CandidatExamen c = candidats.findById(candidatId)
                .filter(x -> x.getExamen().getId().equals(examenId) && x.getEtablissement().getId().equals(etablissementId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Candidat introuvable"));
        if (r.statut() == null || !STATUTS.contains(r.statut())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Statut inconnu : " + r.statut());
        }
        List<EpreuveExamen> liste = epreuves.findByExamenIdOrderByDateEpreuveAscHeureDebutAsc(examenId);
        Map<Long, BigDecimal> notes = new HashMap<>();
        if ("PRESENT".equals(r.statut()) && r.notes() != null) {
            Set<Long> connues = new java.util.HashSet<>();
            liste.forEach(e -> connues.add(e.getId()));
            for (var e : r.notes().entrySet()) {
                if (e.getValue() == null) {
                    continue;
                }
                if (!connues.contains(e.getKey())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Épreuve inconnue pour cet examen");
                }
                if (e.getValue().signum() < 0 || e.getValue().compareTo(NOTE_MAX) > 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Les notes sont comprises entre 0 et 20");
                }
                notes.put(e.getKey(), e.getValue().setScale(2, RoundingMode.HALF_UP));
            }
        }
        c.setStatutEpreuves(r.statut());
        c.getNotes().clear();
        c.getNotes().putAll(notes);
        Map<Long, Integer> coefficients = new HashMap<>();
        liste.forEach(e -> coefficients.put(e.getId(), e.getCoefficient()));
        c.setMoyenne(moyenne(coefficients, notes));
        c.setDecision(decision(r.statut(), c.getMoyenne()));
        return new NotesCandidatsDto.Candidat(c.getId(), (c.getEleve().getPrenom() + " " + c.getEleve().getNom()).trim(),
                c.getEleve().getSexe(), c.getNumeroPv(), c.getStatutEpreuves(), new HashMap<>(c.getNotes()), c.getMoyenne(), c.getDecision());
    }

    /**
     * Moyenne pondérée : somme(note x coefficient) / somme(coefficients), sur 20, à deux décimales. Null tant qu'une épreuve
     * n'est pas notée : une moyenne partielle induirait en erreur.
     */
    static BigDecimal moyenne(Map<Long, Integer> coefficients, Map<Long, BigDecimal> notes) {
        if (coefficients.isEmpty() || !notes.keySet().containsAll(coefficients.keySet())) {
            return null;
        }
        BigDecimal somme = BigDecimal.ZERO;
        int total = 0;
        for (var e : coefficients.entrySet()) {
            somme = somme.add(notes.get(e.getKey()).multiply(BigDecimal.valueOf(e.getValue())));
            total += e.getValue();
        }
        return somme.divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    /** Éliminé pour fraude : non admis ; sinon admis à partir de 10 de moyenne ; rien tant que la moyenne manque. */
    static String decision(String statut, BigDecimal moyenne) {
        if ("ELIMINE".equals(statut)) {
            return "NON_ADMIS";
        }
        if (!"PRESENT".equals(statut) || moyenne == null) {
            return null;
        }
        return moyenne.compareTo(MOYENNE_ADMISSION) >= 0 ? "ADMIS" : "NON_ADMIS";
    }

    /**
     * Établit les résultats de l'établissement (présents, admis, absents, éliminés, filles) à partir de ses candidats.
     * Tous les candidats doivent avoir un statut, et les présents une décision.
     */
    public void consolider(Long examenId, Long etablissementId) {
        ExamenNational examen = examen(examenId);
        verifierOuvert(examen);
        List<CandidatExamen> liste = candidats.deLEtablissement(examenId, etablissementId);
        if (liste.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cet établissement n'a aucun candidat à cet examen");
        }
        long sansStatut = liste.stream().filter(c -> "INSCRIT".equals(c.getStatutEpreuves())).count();
        long sansDecision = liste.stream().filter(c -> "PRESENT".equals(c.getStatutEpreuves()) && c.getDecision() == null).count();
        if (sansStatut > 0 || sansDecision > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, (sansStatut > 0 ? sansStatut + " candidat(s) sans statut (présent, absent ou éliminé)" : "")
                    + (sansStatut > 0 && sansDecision > 0 ? " ; " : "")
                    + (sansDecision > 0 ? sansDecision + " candidat(s) présent(s) dont toutes les épreuves ne sont pas notées" : ""));
        }
        int presentes = 0;
        int admis = 0;
        int absents = 0;
        int elimines = 0;
        int presentesFilles = 0;
        int admisFilles = 0;
        for (CandidatExamen c : liste) {
            boolean fille = "F".equalsIgnoreCase(c.getEleve().getSexe());
            switch (c.getStatutEpreuves()) {
                case "ABSENT" -> absents++;
                case "ELIMINE" -> elimines++;
                default -> {
                    presentes++;
                    presentesFilles += fille ? 1 : 0;
                    if ("ADMIS".equals(c.getDecision())) {
                        admis++;
                        admisFilles += fille ? 1 : 0;
                    }
                }
            }
        }
        examenService.saisir(examenId, etablissementId,
                new ResultatExamenRequest(presentes, admis, absents, elimines, presentesFilles, admisFilles, null));
        audit.enregistrer("RESULTATS_CONSOLIDES", "ExamenNational", examenId,
                Map.of("etablissement", etablissementId, "presentes", presentes, "admis", admis));
    }

    private void verifierOuvert(ExamenNational examen) {
        if ("PROCLAME".equals(examen.getStatutResultats())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Les résultats ont été proclamés : la saisie est close");
        }
    }

    private ExamenNational examen(Long id) {
        return examens.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Examen introuvable"));
    }
}
