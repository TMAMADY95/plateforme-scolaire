package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.CandidatExamen;
import com.plateforme.scolaire.domain.EpreuveExamen;
import com.plateforme.scolaire.web.dto.AnomalieExamenDto;
import com.plateforme.scolaire.web.dto.ControleCoherenceDto;
import com.plateforme.scolaire.repository.CandidatExamenRepository;
import com.plateforme.scolaire.repository.EpreuveExamenRepository;
import com.plateforme.scolaire.repository.ExamenNationalRepository;
import com.plateforme.scolaire.repository.ListeCandidatsRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Contrôle de cohérence (lecture seule) : repère les anomalies des listes de candidats et des notes avant transmission
 * ou validation (numéros de PV, notes manquantes ou suspectes, statuts incohérents, affectations incomplètes).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ControleCoherenceExamenService {

    static final int CANDIDATS_PAR_SALLE = 30;
    static final BigDecimal NOTE_HAUTE = new BigDecimal("19.50");
    static final BigDecimal ECART_SUSPECT = new BigDecimal("10");

    private final ExamenNationalRepository examens;
    private final CandidatExamenRepository candidats;
    private final EpreuveExamenRepository epreuves;
    private final ListeCandidatsRepository listes;

    public ControleCoherenceDto controle(Long etablissementId, Long examenId) {
        var examen = examens.findById(examenId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Examen introuvable"));
        String statut = listes.findByExamenIdAndEtablissementId(examenId, etablissementId).map(l -> l.getStatut()).orElse("BROUILLON");
        List<CandidatExamen> liste = candidats.deLEtablissement(examenId, etablissementId);
        List<EpreuveExamen> eps = epreuves.findByExamenIdOrderByDateEpreuveAscHeureDebutAsc(examenId);
        List<AnomalieExamenDto> anomalies = detecter(liste, eps, examen.getBareme());
        int erreurs = (int) anomalies.stream().filter(a -> "ERREUR".equals(a.gravite())).count();
        return new ControleCoherenceDto(examenId, etablissementId, statut, liste.size(), erreurs, anomalies.size() - erreurs, anomalies);
    }

    /** Pur : détecte les anomalies d'une liste de candidats au regard des épreuves de l'examen. */
    static List<AnomalieExamenDto> detecter(List<CandidatExamen> liste, List<EpreuveExamen> eps) {
        return detecter(liste, eps, 20);
    }

    /** Idem avec le barème de l'examen (10 ou 20) : les seuils d'alerte suivent le barème. */
    static List<AnomalieExamenDto> detecter(List<CandidatExamen> liste, List<EpreuveExamen> eps, int bareme) {
        BigDecimal echelle = BigDecimal.valueOf(bareme).divide(BigDecimal.valueOf(20), 4, java.math.RoundingMode.HALF_UP);
        List<AnomalieExamenDto> res = new ArrayList<>();
        Map<Long, Integer> coefficients = new LinkedHashMap<>();
        eps.forEach(e -> coefficients.put(e.getId(), e.getCoefficient()));

        Map<String, Integer> pv = new HashMap<>();
        Map<Long, Integer> eleves = new HashMap<>();
        Map<String, Integer> places = new HashMap<>();
        Map<String, Integer> effectifSalle = new HashMap<>();
        for (CandidatExamen c : liste) {
            if (c.getNumeroPv() != null && !c.getNumeroPv().isBlank()) {
                pv.merge(c.getNumeroPv().trim(), 1, Integer::sum);
            }
            eleves.merge(c.getEleve().getId(), 1, Integer::sum);
            if (c.getCentre() != null && c.getSalle() != null) {
                effectifSalle.merge(c.getCentre().getId() + "/" + c.getSalle(), 1, Integer::sum);
                if (c.getTableNumero() != null) {
                    places.merge(c.getCentre().getId() + "/" + c.getSalle() + "/" + c.getTableNumero(), 1, Integer::sum);
                }
            }
        }

        // Moyenne par épreuve, pour repérer les notes qui s'écartent fortement de la tendance.
        Map<Long, BigDecimal> moyenneEpreuve = new HashMap<>();
        for (Long id : coefficients.keySet()) {
            var notes = liste.stream().filter(c -> "PRESENT".equals(c.getStatutEpreuves())).map(c -> c.getNotes().get(id))
                    .filter(n -> n != null).toList();
            if (notes.size() >= 5) {
                moyenneEpreuve.put(id, notes.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(notes.size()), 2, java.math.RoundingMode.HALF_UP));
            }
        }

        for (CandidatExamen c : liste) {
            String statut = c.getStatutEpreuves() == null ? "INSCRIT" : c.getStatutEpreuves();
            if (c.getNumeroPv() == null || c.getNumeroPv().isBlank()) {
                ajouter(res, "PV_MANQUANT", "ERREUR", "Numéro de PV manquant", c);
            } else if (pv.get(c.getNumeroPv().trim()) > 1) {
                ajouter(res, "PV_DOUBLON", "ERREUR", "Numéro de PV en double : " + c.getNumeroPv(), c);
            }
            if (eleves.get(c.getEleve().getId()) > 1) {
                ajouter(res, "CANDIDAT_DOUBLON", "ERREUR", "Candidat inscrit plusieurs fois", c);
            }
            if (c.getCentre() == null) {
                ajouter(res, "SANS_CENTRE", "AVERTISSEMENT", "Aucun centre d'examen affecté", c);
            } else if (c.getSalle() == null || c.getTableNumero() == null) {
                ajouter(res, "SANS_PLACE", "AVERTISSEMENT", "Salle ou table non attribuée", c);
            } else {
                String cle = c.getCentre().getId() + "/" + c.getSalle() + "/" + c.getTableNumero();
                if (places.get(cle) > 1) {
                    ajouter(res, "PLACE_DOUBLON", "ERREUR",
                            "Salle " + c.getSalle() + ", table " + c.getTableNumero() + " attribuée à plusieurs candidats", c);
                }
                if (effectifSalle.get(c.getCentre().getId() + "/" + c.getSalle()) > CANDIDATS_PAR_SALLE) {
                    ajouter(res, "SALLE_SURCHARGEE", "AVERTISSEMENT",
                            "Salle " + c.getSalle() + " : plus de " + CANDIDATS_PAR_SALLE + " candidats", c);
                }
            }

            var notes = c.getNotes();
            boolean aDesNotes = coefficients.keySet().stream().anyMatch(id -> notes.get(id) != null);
            if (("ABSENT".equals(statut) || "ELIMINE".equals(statut)) && aDesNotes) {
                ajouter(res, "NOTES_SUR_" + statut, "ERREUR",
                        ("ABSENT".equals(statut) ? "Candidat absent" : "Candidat éliminé") + " mais des notes sont saisies", c);
            }
            if ("PRESENT".equals(statut)) {
                List<String> manquantes = new ArrayList<>();
                for (EpreuveExamen e : eps) {
                    if (notes.get(e.getId()) == null) {
                        manquantes.add(e.nom());
                    }
                }
                if (!manquantes.isEmpty()) {
                    ajouter(res, "NOTES_MANQUANTES", "ERREUR", "Note manquante : " + String.join(", ", manquantes), c);
                }
                for (EpreuveExamen e : eps) {
                    BigDecimal n = notes.get(e.getId());
                    if (n == null) {
                        continue;
                    }
                    if (n.signum() < 0 || n.compareTo(BigDecimal.valueOf(bareme)) > 0) {
                        ajouter(res, "NOTE_HORS_BAREME", "ERREUR", e.nom() + " : note hors barème (" + n + ")", c);
                    } else if (n.signum() == 0) {
                        ajouter(res, "NOTE_ZERO", "AVERTISSEMENT", e.nom() + " : note de 0, à vérifier", c);
                    } else if (n.compareTo(NOTE_HAUTE.multiply(echelle)) >= 0) {
                        ajouter(res, "NOTE_ELEVEE", "AVERTISSEMENT", e.nom() + " : note anormalement élevée (" + n + ")", c);
                    } else if (moyenneEpreuve.containsKey(e.getId())
                            && n.subtract(moyenneEpreuve.get(e.getId())).abs().compareTo(ECART_SUSPECT.multiply(echelle)) >= 0) {
                        ajouter(res, "NOTE_ECART", "AVERTISSEMENT", e.nom() + " : note très éloignée de la moyenne de l'épreuve ("
                                + moyenneEpreuve.get(e.getId()) + ")", c);
                    }
                }
                BigDecimal attendue = NotesExamenService.moyenne(coefficients, notes);
                if (attendue != null && (c.getMoyenne() == null || c.getMoyenne().compareTo(attendue) != 0)) {
                    ajouter(res, "MOYENNE_INCOHERENTE", "ERREUR",
                            "Moyenne enregistrée " + (c.getMoyenne() == null ? "absente" : c.getMoyenne()) + ", attendue " + attendue, c);
                } else if (attendue != null && !java.util.Objects.equals(c.getDecision(), NotesExamenService.decision(statut, attendue, bareme))) {
                    ajouter(res, "DECISION_INCOHERENTE", "ERREUR", "Décision incompatible avec la moyenne " + attendue, c);
                }
            }
            if ("ELIMINE".equals(statut) && !"NON_ADMIS".equals(c.getDecision())) {
                ajouter(res, "DECISION_INCOHERENTE", "ERREUR", "Candidat éliminé non déclaré non admis", c);
            }
        }
        return res;
    }

    private static void ajouter(List<AnomalieExamenDto> res, String code, String gravite, String message, CandidatExamen c) {
        var e = c.getEleve();
        res.add(new AnomalieExamenDto(code, gravite, message, c.getId(), (e.getPrenom() + " " + e.getNom()).trim(), c.getNumero()));
    }
}
