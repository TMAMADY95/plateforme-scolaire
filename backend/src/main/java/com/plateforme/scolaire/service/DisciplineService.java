package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.Eleve;
import com.plateforme.scolaire.domain.IncidentDisciplinaire;
import com.plateforme.scolaire.repository.EleveRepository;
import com.plateforme.scolaire.repository.EtablissementRepository;
import com.plateforme.scolaire.repository.IncidentDisciplinaireRepository;
import com.plateforme.scolaire.repository.InscriptionRepository;
import com.plateforme.scolaire.security.DroitsExamen;
import com.plateforme.scolaire.security.PerimetreService;
import com.plateforme.scolaire.web.dto.DisciplineDto;
import java.time.Clock;
import java.time.LocalDate;
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
 * Discipline scolaire. Les sanctions sont graduées selon la gravité des faits. Les seuils (8 jours d'exclusion temporaire au plus,
 * 3 incidents pour parler de récidive) sont des hypothèses à confirmer. L'exclusion définitive n'annule pas l'inscription
 * automatiquement : la direction doit le faire explicitement.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class DisciplineService {

    static final Set<String> TYPES = Set.of("INSOLENCE", "VIOLENCE", "HARCELEMENT", "FRAUDE", "DEGRADATION", "VOL", "TELEPHONE", "TENUE", "AUTRE");
    static final Set<String> GRAVITES = Set.of("LEGERE", "GRAVE", "TRES_GRAVE");
    static final int JOURS_MAX_EXCLUSION_TEMPORAIRE = 8;
    static final int SEUIL_RECIDIVE = 3;

    private final IncidentDisciplinaireRepository incidents;
    private final EleveRepository eleves;
    private final InscriptionRepository inscriptions;
    private final EtablissementRepository etablissements;
    private final PerimetreService perimetre;
    private final AuditService audit;
    private final Clock clock;

    /** Pur : sanctions permises pour une gravité. */
    static boolean sanctionAutorisee(String gravite, String sanction) {
        return switch (gravite) {
            case "LEGERE" -> Set.of("AVERTISSEMENT", "BLAME", "RETENUE").contains(sanction);
            case "GRAVE" -> Set.of("AVERTISSEMENT", "BLAME", "RETENUE", "EXCLUSION_TEMPORAIRE", "CONSEIL_DISCIPLINE").contains(sanction);
            case "TRES_GRAVE" -> Set.of("BLAME", "EXCLUSION_TEMPORAIRE", "CONSEIL_DISCIPLINE", "EXCLUSION_DEFINITIVE").contains(sanction);
            default -> false;
        };
    }

    /** Pur : message d'erreur d'une sanction invalide, null si elle est correcte. */
    static String verifierSanction(String gravite, String sanction, Integer jours, LocalDate debut, String observation) {
        if (!sanctionAutorisee(gravite, sanction)) {
            return "Cette sanction n'est pas permise pour des faits de gravité " + gravite;
        }
        if ("EXCLUSION_TEMPORAIRE".equals(sanction)) {
            if (jours == null || jours < 1 || jours > JOURS_MAX_EXCLUSION_TEMPORAIRE) {
                return "Une exclusion temporaire dure de 1 à " + JOURS_MAX_EXCLUSION_TEMPORAIRE + " jours";
            }
            if (debut == null) {
                return "La date de début de l'exclusion temporaire est obligatoire";
            }
        } else if (jours != null || debut != null) {
            return "La durée et la date de début ne concernent que l'exclusion temporaire";
        }
        if (("EXCLUSION_DEFINITIVE".equals(sanction) || "CONSEIL_DISCIPLINE".equals(sanction)) && (observation == null || observation.isBlank())) {
            return "Une observation est obligatoire pour cette sanction";
        }
        return null;
    }

    /** Pur : récidive à partir d'un nombre d'incidents non classés dans l'année. */
    static boolean recidive(int nombre) {
        return nombre >= SEUIL_RECIDIVE;
    }

    private static ResponseStatusException erreur(HttpStatus s, String m) {
        return new ResponseStatusException(s, m);
    }

    private DisciplineDto.Incident dto(IncidentDisciplinaire i, Map<Long, Eleve> parId, Map<String, Integer> compte) {
        Eleve e = parId.computeIfAbsent(i.getEleveId(), id -> eleves.findById(id).orElseThrow());
        int n = compte.getOrDefault(i.getEleveId() + "-" + i.getAnneeScolaireId(), 0);
        return new DisciplineDto.Incident(i.getId(), e.getId(), e.getMatricule(), e.getPrenom() + " " + e.getNom(), i.getDateFaits(), i.getType(), i.getGravite(),
                i.getDescription(), i.getStatut(), i.getSanction(), i.getExclusionJours(), i.getExclusionDebut(), i.getObservation(), i.isParentsInformes(),
                i.getSignalePar(), i.getSignaleLe(), i.getDecidePar(), i.getDecideLe(), n, recidive(n));
    }

    private Map<String, Integer> compter(List<IncidentDisciplinaire> liste) {
        Map<String, Integer> res = new HashMap<>();
        liste.stream().filter(i -> !"CLASSE".equals(i.getStatut())).forEach(i -> res.merge(i.getEleveId() + "-" + i.getAnneeScolaireId(), 1, Integer::sum));
        return res;
    }

    @Transactional(readOnly = true)
    public DisciplineDto lister(Long etablissementId, Long anneeId) {
        if (!etablissements.existsById(etablissementId)) {
            throw erreur(HttpStatus.NOT_FOUND, "Établissement introuvable");
        }
        List<IncidentDisciplinaire> liste = incidents.findByEtablissementIdAndAnneeScolaireIdOrderByDateFaitsDescIdDesc(etablissementId, anneeId);
        Map<String, Integer> compte = compter(liste);
        Map<Long, Eleve> cache = new HashMap<>();
        List<DisciplineDto.Incident> lignes = liste.stream().map(i -> dto(i, cache, compte)).toList();
        int exclusions = (int) liste.stream().filter(i -> i.getSanction() != null && i.getSanction().startsWith("EXCLUSION")).count();
        int recidivistes = (int) compte.values().stream().filter(DisciplineService::recidive).count();
        return new DisciplineDto(etablissementId, anneeId, new DisciplineDto.Resume(liste.size(), (int) liste.stream().filter(i -> "OUVERT".equals(i.getStatut())).count(),
                (int) liste.stream().filter(i -> "SANCTIONNE".equals(i.getStatut())).count(), (int) liste.stream().filter(i -> "CLASSE".equals(i.getStatut())).count(),
                exclusions, recidivistes), lignes);
    }

    /** Discipline d'un élève : la direction voit tout, un parent ou l'élève seulement les faits sanctionnés. */
    @Transactional(readOnly = true)
    public List<DisciplineDto.Incident> parEleve(Long eleveId) {
        boolean direction = DroitsExamen.aRole("DIRECTION_ETABLISSEMENT");
        List<IncidentDisciplinaire> liste = incidents.findByEleveIdOrderByDateFaitsDescIdDesc(eleveId);
        Map<String, Integer> compte = compter(liste);
        Map<Long, Eleve> cache = new HashMap<>();
        return liste.stream().filter(i -> direction ? perimetre.peutAccederEtablissement(i.getEtablissementId()) : "SANCTIONNE".equals(i.getStatut()))
                .map(i -> dto(i, cache, compte)).toList();
    }

    public DisciplineDto.Incident signaler(Long etablissementId, DisciplineDto.Signalement q, String auteur) {
        if (!etablissements.existsById(etablissementId)) {
            throw erreur(HttpStatus.NOT_FOUND, "Établissement introuvable");
        }
        if (!TYPES.contains(q.type())) {
            throw erreur(HttpStatus.BAD_REQUEST, "Type d'incident inconnu : " + q.type());
        }
        if (!GRAVITES.contains(q.gravite())) {
            throw erreur(HttpStatus.BAD_REQUEST, "Gravité inconnue : " + q.gravite());
        }
        LocalDate aujourdhui = LocalDate.now(clock);
        if (q.dateFaits().isAfter(aujourdhui)) {
            throw erreur(HttpStatus.BAD_REQUEST, "La date des faits ne peut pas être dans le futur");
        }
        Eleve eleve = IdentifiantNational.trouver(eleves, q.matriculeEleve()).orElseThrow(() -> erreur(HttpStatus.NOT_FOUND, "Aucun élève avec cet identifiant"));
        boolean inscrit = inscriptions.findByEleveIdAndAnneeScolaireIdAndStatutIn(eleve.getId(), q.anneeId(), List.of("ACCEPTEE"))
                .filter(i -> i.getEtablissement().getId().equals(etablissementId)).isPresent();
        if (!inscrit) {
            throw erreur(HttpStatus.CONFLICT, "Cet élève n'est pas inscrit dans votre établissement pour cette année");
        }
        IncidentDisciplinaire i = new IncidentDisciplinaire();
        i.setEleveId(eleve.getId());
        i.setEtablissementId(etablissementId);
        i.setAnneeScolaireId(q.anneeId());
        i.setDateFaits(q.dateFaits());
        i.setType(q.type());
        i.setGravite(q.gravite());
        i.setDescription(q.description().trim());
        i.setSignalePar(auteur);
        i.setSignaleLe(aujourdhui);
        incidents.save(i);
        audit.enregistrer("INCIDENT_DISCIPLINAIRE_SIGNALE", "Etablissement", etablissementId, Map.of("incident", i.getId(), "eleve", eleve.getId(), "gravite", q.gravite()));
        List<IncidentDisciplinaire> liste = incidents.findByEleveIdOrderByDateFaitsDescIdDesc(eleve.getId());
        return dto(i, new HashMap<>(Map.of(eleve.getId(), eleve)), compter(liste));
    }

    private IncidentDisciplinaire charger(Long id) {
        IncidentDisciplinaire i = incidents.findById(id).orElseThrow(() -> erreur(HttpStatus.NOT_FOUND, "Incident introuvable"));
        if (!perimetre.peutGererEtablissement(i.getEtablissementId())) {
            throw erreur(HttpStatus.FORBIDDEN, "Accès refusé");
        }
        if (!"OUVERT".equals(i.getStatut())) {
            throw erreur(HttpStatus.CONFLICT, "Cet incident a déjà été traité");
        }
        return i;
    }

    private DisciplineDto.Incident apres(IncidentDisciplinaire i) {
        return dto(i, new HashMap<>(), compter(incidents.findByEleveIdOrderByDateFaitsDescIdDesc(i.getEleveId())));
    }

    public DisciplineDto.Incident sanctionner(Long id, DisciplineDto.Sanction q, String auteur) {
        IncidentDisciplinaire i = charger(id);
        String message = verifierSanction(i.getGravite(), q.sanction(), q.exclusionJours(), q.exclusionDebut(), q.observation());
        if (message != null) {
            throw erreur(HttpStatus.BAD_REQUEST, message);
        }
        i.setStatut("SANCTIONNE");
        i.setSanction(q.sanction());
        i.setExclusionJours(q.exclusionJours());
        i.setExclusionDebut(q.exclusionDebut());
        i.setObservation(q.observation() == null || q.observation().isBlank() ? null : q.observation().trim());
        i.setParentsInformes(q.parentsInformes());
        i.setDecidePar(auteur);
        i.setDecideLe(LocalDate.now(clock));
        audit.enregistrer("INCIDENT_DISCIPLINAIRE_SANCTIONNE", "Etablissement", i.getEtablissementId(), Map.of("incident", id, "sanction", q.sanction()));
        return apres(i);
    }

    public DisciplineDto.Incident classer(Long id, DisciplineDto.Classement q, String auteur) {
        IncidentDisciplinaire i = charger(id);
        i.setStatut("CLASSE");
        i.setObservation(q.motif().trim());
        i.setDecidePar(auteur);
        i.setDecideLe(LocalDate.now(clock));
        audit.enregistrer("INCIDENT_DISCIPLINAIRE_CLASSE", "Etablissement", i.getEtablissementId(), Map.of("incident", id));
        return apres(i);
    }
}
