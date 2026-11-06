package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.AnneeScolaire;
import com.plateforme.scolaire.domain.DecisionFinAnnee;
import com.plateforme.scolaire.domain.Inscription;
import com.plateforme.scolaire.repository.AnneeScolaireRepository;
import com.plateforme.scolaire.repository.DecisionFinAnneeRepository;
import com.plateforme.scolaire.repository.EtablissementRepository;
import com.plateforme.scolaire.repository.InscriptionRepository;
import com.plateforme.scolaire.web.dto.RentreeDto;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
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
 * Rentrée : à partir des décisions du conseil de classe, crée les inscriptions de l'année suivante (en attente) des élèves qui passent
 * ou redoublent. La direction les accepte ensuite avec une classe. Les élèves exclus ne sont pas réinscrits ; ceux qui passent depuis le
 * dernier niveau sont en fin de parcours. Les classes d'examen (CEE, BEPC, baccalauréat) dépendent aussi des résultats d'examen : à valider.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class RentreeService {

    private final InscriptionRepository inscriptions;
    private final DecisionFinAnneeRepository decisions;
    private final AnneeScolaireRepository annees;
    private final EtablissementRepository etablissements;
    private final EntityManager em;
    private final AuditService audit;
    private final Clock clock;

    /** Pur : A_REINSCRIRE, FIN_DE_PARCOURS (passage depuis le dernier niveau), EXCLU ou SANS_DECISION. */
    static String classer(String decision, int ordreNiveau, int ordreMax) {
        if (decision == null) {
            return "SANS_DECISION";
        }
        return switch (decision) {
            case "REDOUBLEMENT" -> "A_REINSCRIRE";
            case "PASSAGE" -> ordreNiveau >= ordreMax ? "FIN_DE_PARCOURS" : "A_REINSCRIRE";
            default -> "EXCLU";
        };
    }

    /** Pur : année dont le début suit immédiatement celui de l'année donnée ; null si aucune. */
    static AnneeScolaire suivante(List<AnneeScolaire> toutes, AnneeScolaire courante) {
        return toutes.stream().filter(a -> a.getDateDebut().isAfter(courante.getDateDebut())).min(Comparator.comparing(AnneeScolaire::getDateDebut)).orElse(null);
    }

    private int ordreMax() {
        Number n = (Number) em.createQuery("select max(n.ordre) from Niveau n").getSingleResult();
        return n == null ? 0 : n.intValue();
    }

    private record Ligne(Inscription inscription, String decision, String categorie) {
    }

    private List<Ligne> lignes(Long etablissementId, Long anneeId) {
        List<Inscription> actives = inscriptions.findByEtablissementIdAndAnneeScolaireIdOrderByDateDemandeDescIdDesc(etablissementId, anneeId).stream()
                .filter(i -> "ACCEPTEE".equals(i.getStatut()) && i.getClasse() != null).toList();
        Map<Long, String> parInscription = new HashMap<>();
        if (!actives.isEmpty()) {
            decisions.findByInscriptionIdIn(actives.stream().map(Inscription::getId).toList()).forEach(d -> parInscription.put(d.getInscriptionId(), d.getDecision()));
        }
        int max = ordreMax();
        List<Ligne> res = new ArrayList<>();
        for (Inscription i : actives) {
            String d = parInscription.get(i.getId());
            res.add(new Ligne(i, d, classer(d, i.getClasse().getNiveau().getOrdre(), max)));
        }
        return res;
    }

    private AnneeScolaire suivante(AnneeScolaire courante) {
        return suivante(annees.findAllByOrderByDateDebutDesc(), courante);
    }

    private boolean dejaInscrit(Inscription i, Long anneeSuivanteId) {
        return inscriptions.findByEleveIdAndAnneeScolaireIdAndStatutIn(i.getEleve().getId(), anneeSuivanteId, List.of("EN_ATTENTE", "ACCEPTEE")).isPresent();
    }

    @Transactional(readOnly = true)
    public RentreeDto apercu(Long etablissementId, Long anneeId) {
        if (!etablissements.existsById(etablissementId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Établissement introuvable");
        }
        AnneeScolaire annee = annees.findById(anneeId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Année scolaire introuvable"));
        AnneeScolaire suivante = suivante(annee);
        Map<Long, int[]> parClasse = new LinkedHashMap<>();
        Map<Long, String[]> noms = new HashMap<>();
        int aReinscrire = 0;
        int deja = 0;
        int fin = 0;
        int exclus = 0;
        int sans = 0;
        for (Ligne l : lignes(etablissementId, anneeId)) {
            Long cid = l.inscription().getClasse().getId();
            noms.putIfAbsent(cid, new String[] {l.inscription().getClasse().getNom(), l.inscription().getClasse().getNiveau().getLibelle()});
            int[] c = parClasse.computeIfAbsent(cid, k -> new int[5]);
            switch (l.categorie()) {
                case "A_REINSCRIRE" -> {
                    if ("PASSAGE".equals(l.decision())) {
                        c[0]++;
                    } else {
                        c[1]++;
                    }
                    if (suivante != null && dejaInscrit(l.inscription(), suivante.getId())) {
                        deja++;
                    } else {
                        aReinscrire++;
                    }
                }
                case "FIN_DE_PARCOURS" -> {
                    c[4]++;
                    fin++;
                }
                case "EXCLU" -> {
                    c[2]++;
                    exclus++;
                }
                default -> {
                    c[3]++;
                    sans++;
                }
            }
        }
        List<RentreeDto.Classe> classes = parClasse.entrySet().stream()
                .map(e -> new RentreeDto.Classe(e.getKey(), noms.get(e.getKey())[0], noms.get(e.getKey())[1], e.getValue()[0], e.getValue()[1], e.getValue()[2],
                        e.getValue()[3], e.getValue()[4]))
                .sorted(Comparator.comparing(RentreeDto.Classe::classe)).toList();
        return new RentreeDto(etablissementId, anneeId, suivante == null ? null : suivante.getId(), suivante == null ? null : suivante.getLibelle(),
                new RentreeDto.Resume(aReinscrire, deja, fin, exclus, sans), classes);
    }

    public RentreeDto.Preparation preparer(Long etablissementId, Long anneeId) {
        AnneeScolaire annee = annees.findById(anneeId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Année scolaire introuvable"));
        AnneeScolaire suivante = suivante(annee);
        if (suivante == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Aucune année scolaire postérieure n'est définie : créez d'abord l'année suivante");
        }
        int crees = 0;
        for (Ligne l : lignes(etablissementId, anneeId)) {
            if (!"A_REINSCRIRE".equals(l.categorie()) || dejaInscrit(l.inscription(), suivante.getId())) {
                continue;
            }
            Inscription n = new Inscription();
            n.setEleve(l.inscription().getEleve());
            n.setEtablissement(l.inscription().getEtablissement());
            n.setAnneeScolaire(suivante);
            n.setStatut("EN_ATTENTE");
            n.setDateDemande(LocalDate.now(clock));
            inscriptions.save(n);
            crees++;
        }
        audit.enregistrer("RENTREE_PREPAREE", "Etablissement", etablissementId, Map.of("annee", anneeId, "anneeSuivante", suivante.getId(), "inscriptions", crees));
        return new RentreeDto.Preparation(crees, apercu(etablissementId, anneeId));
    }
}
