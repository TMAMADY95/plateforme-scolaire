package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.Classe;
import com.plateforme.scolaire.domain.DecisionFinAnnee;
import com.plateforme.scolaire.repository.ClasseRepository;
import com.plateforme.scolaire.repository.DecisionFinAnneeRepository;
import com.plateforme.scolaire.web.dto.ConseilClasseDto;
import com.plateforme.scolaire.web.dto.LigneResultatDto;
import com.plateforme.scolaire.web.dto.PeriodeDto;
import com.plateforme.scolaire.web.dto.ResultatsClasseDto;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
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
 * Conseil de classe de fin d'année. La moyenne annuelle est la moyenne des moyennes générales des périodes de l'établissement
 * (poids égaux). Seuils : 10 pour passer, entre 8 et 10 le conseil délibère, sous 8 le redoublement est suggéré ; ce sont des
 * hypothèses à confirmer avec le ministère. Une décision qui s'écarte de la suggestion doit être motivée.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ConseilClasseService {

    static final BigDecimal SEUIL_PASSAGE = BigDecimal.TEN;
    static final BigDecimal SEUIL_DELIBERATION = BigDecimal.valueOf(8);
    static final Set<String> DECISIONS = Set.of("PASSAGE", "REDOUBLEMENT", "EXCLUSION");

    private final ClasseRepository classes;
    private final MoyennesService moyennes;
    private final PeriodesEtablissementService periodes;
    private final DecisionFinAnneeRepository decisions;
    private final AuditService audit;
    private final Clock clock;

    /** Pur : moyenne des moyennes de période renseignées, à 2 décimales ; null si aucune. */
    static BigDecimal moyenneAnnuelle(List<BigDecimal> moyennesPeriodes) {
        List<BigDecimal> valeurs = moyennesPeriodes.stream().filter(m -> m != null).toList();
        if (valeurs.isEmpty()) {
            return null;
        }
        return valeurs.stream().reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(valeurs.size()), 2, RoundingMode.HALF_UP);
    }

    /** Pur : ADMIS, A_DELIBERER ou REDOUBLE ; null sans moyenne. */
    static String suggestion(BigDecimal moyenne) {
        if (moyenne == null) {
            return null;
        }
        if (moyenne.compareTo(SEUIL_PASSAGE) >= 0) {
            return "ADMIS";
        }
        return moyenne.compareTo(SEUIL_DELIBERATION) >= 0 ? "A_DELIBERER" : "REDOUBLE";
    }

    /** Pur : mention de la moyenne annuelle. */
    static String mention(BigDecimal moyenne) {
        if (moyenne == null) {
            return null;
        }
        if (moyenne.compareTo(BigDecimal.valueOf(16)) >= 0) {
            return "FELICITATIONS";
        }
        if (moyenne.compareTo(BigDecimal.valueOf(14)) >= 0) {
            return "ENCOURAGEMENTS";
        }
        return moyenne.compareTo(BigDecimal.valueOf(12)) >= 0 ? "TABLEAU_HONNEUR" : null;
    }

    /** Pur : décision directement déduite de la suggestion ; null quand le conseil doit trancher. */
    static String decisionSuggeree(String suggestion) {
        return switch (suggestion == null ? "" : suggestion) {
            case "ADMIS" -> "PASSAGE";
            case "REDOUBLE" -> "REDOUBLEMENT";
            default -> null;
        };
    }

    /** Pur : une décision contraire à la suggestion, une exclusion ou une décision sans moyenne doit être motivée. */
    static boolean observationRequise(String decision, String suggestion) {
        if ("EXCLUSION".equals(decision) || suggestion == null) {
            return true;
        }
        return ("PASSAGE".equals(decision) && "REDOUBLE".equals(suggestion)) || ("REDOUBLEMENT".equals(decision) && "ADMIS".equals(suggestion));
    }

    /** Pur : rang de classement (ex æquo à égalité de moyenne), null sans moyenne. */
    static Map<Long, Integer> rangs(Map<Long, BigDecimal> moyennesParInscription) {
        List<Map.Entry<Long, BigDecimal>> tri = moyennesParInscription.entrySet().stream().filter(e -> e.getValue() != null)
                .sorted(Map.Entry.<Long, BigDecimal>comparingByValue().reversed()).toList();
        Map<Long, Integer> res = new HashMap<>();
        BigDecimal precedente = null;
        int rang = 0;
        for (int i = 0; i < tri.size(); i++) {
            if (precedente == null || tri.get(i).getValue().compareTo(precedente) != 0) {
                rang = i + 1;
                precedente = tri.get(i).getValue();
            }
            res.put(tri.get(i).getKey(), rang);
        }
        return res;
    }

    private Classe classe(Long classeId) {
        return classes.findById(classeId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Classe introuvable"));
    }

    @Transactional(readOnly = true)
    public ConseilClasseDto conseil(Long classeId) {
        Classe classe = classe(classeId);
        List<PeriodeDto> ps = periodes.deLaClasse(classeId);
        if (ps.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Aucune période n'est définie pour cette classe : définissez d'abord les trimestres ou semestres");
        }
        Map<Long, DecisionFinAnnee> parInscription = new HashMap<>();
        decisions.findByClasseId(classeId).forEach(d -> parInscription.put(d.getInscriptionId(), d));

        Map<Long, LigneResultatDto> eleves = new LinkedHashMap<>();
        Map<Long, Map<Long, BigDecimal>> parPeriode = new HashMap<>();
        for (PeriodeDto p : ps) {
            ResultatsClasseDto r = moyennes.resultatsClasse(classeId, p.id());
            for (LigneResultatDto l : r.eleves()) {
                eleves.putIfAbsent(l.inscriptionId(), l);
                parPeriode.computeIfAbsent(l.inscriptionId(), k -> new LinkedHashMap<>()).put(p.id(), l.moyenneGenerale());
            }
        }
        Map<Long, BigDecimal> annuelles = new HashMap<>();
        eleves.keySet().forEach(id -> annuelles.put(id, moyenneAnnuelle(new ArrayList<>(parPeriode.getOrDefault(id, Map.of()).values()))));
        Map<Long, Integer> rangs = rangs(annuelles);

        List<ConseilClasseDto.Ligne> lignes = new ArrayList<>();
        int admis = 0;
        int adel = 0;
        int redouble = 0;
        int evalues = 0;
        int passages = 0;
        int redoublements = 0;
        int exclusions = 0;
        for (LigneResultatDto l : eleves.values()) {
            BigDecimal m = annuelles.get(l.inscriptionId());
            String sug = suggestion(m);
            DecisionFinAnnee d = parInscription.get(l.inscriptionId());
            if (m != null) {
                evalues++;
            }
            switch (sug == null ? "" : sug) {
                case "ADMIS" -> admis++;
                case "A_DELIBERER" -> adel++;
                case "REDOUBLE" -> redouble++;
                default -> {
                }
            }
            if (d != null) {
                switch (d.getDecision()) {
                    case "PASSAGE" -> passages++;
                    case "REDOUBLEMENT" -> redoublements++;
                    default -> exclusions++;
                }
            }
            lignes.add(new ConseilClasseDto.Ligne(l.inscriptionId(), l.eleve(), parPeriode.getOrDefault(l.inscriptionId(), Map.of()), m, rangs.get(l.inscriptionId()),
                    sug, mention(m), d == null ? null : d.getDecision(), d == null ? null : d.getObservation()));
        }
        lignes.sort(Comparator.comparing((ConseilClasseDto.Ligne x) -> x.rang() == null ? Integer.MAX_VALUE : x.rang())
                .thenComparing(x -> x.eleve().nom()).thenComparing(x -> x.eleve().prenom()));
        int decides = passages + redoublements + exclusions;
        BigDecimal taux = decides == 0 ? null : BigDecimal.valueOf(passages * 100L).divide(BigDecimal.valueOf(decides), 1, RoundingMode.HALF_UP);
        return new ConseilClasseDto(classeId, classe.getNom(), classe.getNiveau().getLibelle(),
                ps.stream().map(p -> new ConseilClasseDto.Periode(p.id(), p.libelle())).toList(),
                new ConseilClasseDto.Resume(lignes.size(), evalues, admis, adel, redouble, decides, passages, redoublements, exclusions, taux), lignes);
    }

    public ConseilClasseDto decider(Long classeId, ConseilClasseDto.Requete q, String auteur) {
        classe(classeId);
        Map<Long, ConseilClasseDto.Ligne> actuel = new HashMap<>();
        conseil(classeId).eleves().forEach(l -> actuel.put(l.inscriptionId(), l));
        for (ConseilClasseDto.Decision d : q.decisions()) {
            ConseilClasseDto.Ligne l = actuel.get(d.inscriptionId());
            if (l == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Élève inconnu dans cette classe (inscription " + d.inscriptionId() + ")");
            }
            String nom = l.eleve().prenom() + " " + l.eleve().nom();
            if (d.decision() == null || d.decision().isBlank()) {
                decisions.findByInscriptionId(d.inscriptionId()).ifPresent(decisions::delete);
                continue;
            }
            if (!DECISIONS.contains(d.decision())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Décision inconnue : " + d.decision());
            }
            boolean motive = d.observation() != null && !d.observation().isBlank();
            if (observationRequise(d.decision(), l.suggestion()) && !motive) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Une observation est obligatoire pour " + nom + " (décision contraire à la suggestion, exclusion ou absence de moyenne)");
            }
            DecisionFinAnnee e = decisions.findByInscriptionId(d.inscriptionId()).orElseGet(() -> {
                DecisionFinAnnee n = new DecisionFinAnnee();
                n.setInscriptionId(d.inscriptionId());
                n.setClasseId(classeId);
                return n;
            });
            e.setMoyenneAnnuelle(l.moyenneAnnuelle());
            e.setSuggestion(l.suggestion());
            e.setDecision(d.decision());
            e.setObservation(motive ? d.observation().trim() : null);
            e.setDecideLe(LocalDate.now(clock));
            e.setDecidePar(auteur);
            decisions.save(e);
        }
        audit.enregistrer("CONSEIL_CLASSE_DECISIONS", "Classe", classeId, Map.of("decisions", q.decisions().size()));
        return conseil(classeId);
    }

    /** Applique les décisions évidentes (ADMIS → passage, REDOUBLE → redoublement) aux élèves qui n'en ont pas encore ; les cas à délibérer restent au conseil. */
    public ConseilClasseDto appliquerSuggestions(Long classeId, String auteur) {
        List<ConseilClasseDto.Decision> aPrendre = new ArrayList<>();
        for (ConseilClasseDto.Ligne l : conseil(classeId).eleves()) {
            String s = decisionSuggeree(l.suggestion());
            if (l.decision() == null && s != null) {
                aPrendre.add(new ConseilClasseDto.Decision(l.inscriptionId(), s, null));
            }
        }
        if (aPrendre.isEmpty()) {
            return conseil(classeId);
        }
        return decider(classeId, new ConseilClasseDto.Requete(aPrendre), auteur);
    }
}
