package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.AnneeScolaire;
import com.plateforme.scolaire.domain.Periode;
import com.plateforme.scolaire.repository.AnneeScolaireRepository;
import com.plateforme.scolaire.repository.ClasseRepository;
import com.plateforme.scolaire.repository.EtablissementRepository;
import com.plateforme.scolaire.repository.EvaluationRepository;
import com.plateforme.scolaire.repository.PeriodeRepository;
import com.plateforme.scolaire.security.PerimetreService;
import com.plateforme.scolaire.web.dto.PeriodeDto;
import com.plateforme.scolaire.web.dto.PeriodeEtablissementRequest;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Périodes d'évaluation propres à un établissement : trimestres ou semestres de son choix. Elles s'appliquent à toutes
 * ses classes, classes d'examen comprises. Sans période propre, ce sont celles du ministère.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PeriodesEtablissementService {

    private final PeriodeRepository periodes;
    private final AnneeScolaireRepository annees;
    private final EtablissementRepository etablissements;
    private final ClasseRepository classes;
    private final EvaluationRepository evaluations;
    private final PerimetreService perimetre;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public List<PeriodeDto> deLEtablissement(Long etablissementId, Long anneeId) {
        return PeriodesApplicables.pour(periodes.findByAnneeScolaireIdOrderByOrdre(anneeId), etablissementId).stream()
                .map(PeriodeDto::de).toList();
    }

    @Transactional(readOnly = true)
    public List<PeriodeDto> deLaClasse(Long classeId) {
        var classe = classes.findById(classeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Classe introuvable"));
        return deLEtablissement(classe.getEtablissement().getId(), classe.getAnneeScolaire().getId());
    }

    /** Découpe l'année en 3 trimestres ou 2 semestres de durées égales. */
    public List<PeriodeDto> generer(Long etablissementId, Long anneeId, String type) {
        verifierGestion(etablissementId);
        int nombre = switch (type == null ? "" : type.toUpperCase()) {
            case "TRIMESTRE" -> 3;
            case "SEMESTRE" -> 2;
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Type inconnu : TRIMESTRE ou SEMESTRE");
        };
        AnneeScolaire annee = annee(anneeId);
        if (!propres(etablissementId, anneeId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cet établissement a déjà ses propres périodes : modifiez-les ou rétablissez celles du ministère");
        }
        String libelle = nombre == 3 ? "Trimestre" : "Semestre";
        List<Periode> creees = new ArrayList<>();
        for (Periode p : decouper(annee.getDateDebut(), annee.getDateFin(), nombre, libelle)) {
            p.setAnneeScolaire(annee);
            p.setEtablissement(etablissements.getReferenceById(etablissementId));
            p.setTypePeriode(type.toUpperCase());
            creees.add(periodes.save(p));
        }
        audit.enregistrer("PERIODES_GENEREES", "Etablissement", etablissementId, Map.of("type", type.toUpperCase(), "annee", anneeId));
        return creees.stream().map(PeriodeDto::de).toList();
    }

    public PeriodeDto creer(Long etablissementId, Long anneeId, PeriodeEtablissementRequest r) {
        verifierGestion(etablissementId);
        AnneeScolaire annee = annee(anneeId);
        verifier(annee, r);
        if (propres(etablissementId, anneeId).stream().anyMatch(p -> p.getOrdre() == r.ordre())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Une période a déjà ce numéro d'ordre");
        }
        Periode p = new Periode();
        p.setAnneeScolaire(annee);
        p.setEtablissement(etablissements.getReferenceById(etablissementId));
        appliquer(p, r);
        return PeriodeDto.de(periodes.save(p));
    }

    public PeriodeDto modifier(Long periodeId, PeriodeEtablissementRequest r) {
        Periode p = propre(periodeId);
        verifierGestion(p.getEtablissement().getId());
        verifier(p.getAnneeScolaire(), r);
        if (propres(p.getEtablissement().getId(), p.getAnneeScolaire().getId()).stream()
                .anyMatch(autre -> !autre.getId().equals(periodeId) && autre.getOrdre() == r.ordre())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Une période a déjà ce numéro d'ordre");
        }
        appliquer(p, r);
        return PeriodeDto.de(p);
    }

    public void supprimer(Long periodeId) {
        Periode p = propre(periodeId);
        verifierGestion(p.getEtablissement().getId());
        if (evaluations.existsByPeriodeId(periodeId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Des évaluations sont rattachées à cette période");
        }
        periodes.delete(p);
    }

    /** Supprime toutes les périodes propres de l'année : l'établissement retrouve celles du ministère. */
    public void retablir(Long etablissementId, Long anneeId) {
        verifierGestion(etablissementId);
        List<Periode> propres = propres(etablissementId, anneeId);
        if (propres.stream().anyMatch(p -> evaluations.existsByPeriodeId(p.getId()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Des évaluations sont déjà rattachées à ces périodes : elles ne peuvent pas être supprimées");
        }
        periodes.deleteAll(propres);
        audit.enregistrer("PERIODES_RETABLIES", "Etablissement", etablissementId, Map.of("annee", anneeId));
    }

    /** [nombre] périodes consécutives et de durées égales (au jour près) couvrant exactement [debut, fin]. */
    static List<Periode> decouper(LocalDate debut, LocalDate fin, int nombre, String libelle) {
        long jours = ChronoUnit.DAYS.between(debut, fin) + 1;
        List<Periode> resultat = new ArrayList<>();
        LocalDate courant = debut;
        for (int i = 1; i <= nombre; i++) {
            long part = jours / nombre + (i <= jours % nombre ? 1 : 0);
            LocalDate finPeriode = i == nombre ? fin : courant.plusDays(part - 1);
            Periode p = new Periode();
            p.setLibelle(libelle + " " + i);
            p.setOrdre(i);
            p.setDateDebut(courant);
            p.setDateFin(finPeriode);
            resultat.add(p);
            courant = finPeriode.plusDays(1);
        }
        return resultat;
    }

    private List<Periode> propres(Long etablissementId, Long anneeId) {
        return periodes.findByAnneeScolaireIdOrderByOrdre(anneeId).stream()
                .filter(p -> p.getEtablissement() != null && etablissementId.equals(p.getEtablissement().getId())).toList();
    }

    private Periode propre(Long id) {
        Periode p = periodes.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Période introuvable"));
        if (p.getEtablissement() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Période nationale : le ministère la gère");
        }
        return p;
    }

    private AnneeScolaire annee(Long id) {
        return annees.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Année scolaire introuvable"));
    }

    private void verifierGestion(Long etablissementId) {
        if (!perimetre.peutGererEtablissement(etablissementId)) {
            throw new AccessDeniedException("Établissement hors de votre périmètre");
        }
    }

    private static void verifier(AnneeScolaire annee, PeriodeEtablissementRequest r) {
        if (!r.dateFin().isAfter(r.dateDebut())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La date de fin doit suivre la date de début");
        }
        if (r.dateDebut().isBefore(annee.getDateDebut()) || r.dateFin().isAfter(annee.getDateFin())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La période doit être comprise dans l'année scolaire");
        }
    }

    private static void appliquer(Periode p, PeriodeEtablissementRequest r) {
        p.setLibelle(r.libelle().trim());
        p.setOrdre(r.ordre());
        p.setDateDebut(r.dateDebut());
        p.setDateFin(r.dateFin());
        String type = r.typePeriode() == null ? "AUTRE" : r.typePeriode().toUpperCase();
        p.setTypePeriode(List.of("TRIMESTRE", "SEMESTRE").contains(type) ? type : "AUTRE");
    }
}
