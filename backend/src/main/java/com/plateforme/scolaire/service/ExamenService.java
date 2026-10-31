package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.CycleScolaire;
import com.plateforme.scolaire.domain.Etablissement;
import com.plateforme.scolaire.domain.ExamenNational;
import com.plateforme.scolaire.domain.ResultatExamen;
import com.plateforme.scolaire.repository.AnneeScolaireRepository;
import com.plateforme.scolaire.repository.CycleScolaireRepository;
import com.plateforme.scolaire.repository.EtablissementRepository;
import com.plateforme.scolaire.repository.ExamenNationalRepository;
import com.plateforme.scolaire.repository.NiveauRepository;
import com.plateforme.scolaire.repository.ResultatExamenRepository;
import com.plateforme.scolaire.repository.ZoneGeographiqueRepository;
import com.plateforme.scolaire.domain.Niveau;
import com.plateforme.scolaire.domain.ZoneGeographique;
import com.plateforme.scolaire.web.dto.ExamenDto;
import com.plateforme.scolaire.web.dto.ExamenRequest;
import com.plateforme.scolaire.web.dto.RapportExamenDto;
import com.plateforme.scolaire.web.dto.ResultatExamenRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Examens nationaux : le ministère les crée ; ministère et directions régionales saisissent les présentés et les
 * admis de chaque établissement de leur périmètre. Un examen propre à un cycle ne concerne que les établissements
 * qui proposent ce cycle.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ExamenService {

    private final ExamenNationalRepository examenRepository;
    private final ResultatExamenRepository resultatRepository;
    private final AnneeScolaireRepository anneeRepository;
    private final CycleScolaireRepository cycleRepository;
    private final NiveauRepository niveauRepository;
    private final ZoneGeographiqueRepository zoneRepository;
    private final EtablissementRepository etablissementRepository;
    private final RegionService regionService;
    private final AuditService auditService;
    private final com.plateforme.scolaire.security.PerimetreService perimetreService;
    private final com.plateforme.scolaire.repository.TypeEtablissementRepository typeRepository;

    static final String NATIONAL = "NATIONAL";

    /** Niveaux qui peuvent organiser un examen : le national, puis les types de zone réellement présents au référentiel. */
    @Transactional(readOnly = true)
    public List<String> niveauxOrganisation() {
        List<String> niveaux = new ArrayList<>(List.of(NATIONAL));
        zoneRepository.typesDeZone().stream().filter(t -> !NATIONAL.equalsIgnoreCase(t)).forEach(niveaux::add);
        return niveaux;
    }

    /**
     * La zone qui organise l'examen pour cet établissement : la zone ancêtre (ou la zone elle-même) du type voulu.
     * Null pour un examen national ou si l'établissement n'a pas de zone de ce type au-dessus de lui.
     */
    static String organisateur(Etablissement e, String niveauOrganisation) {
        ZoneGeographique z = zoneOrganisatrice(e, niveauOrganisation);
        return z == null ? null : z.getNom();
    }

    static ZoneGeographique zoneOrganisatrice(Etablissement e, String niveauOrganisation) {
        if (niveauOrganisation == null || NATIONAL.equalsIgnoreCase(niveauOrganisation)) {
            return null;
        }
        for (ZoneGeographique z = e.getZone(); z != null; z = z.getParent()) {
            if (niveauOrganisation.equalsIgnoreCase(z.getTypeZone())) {
                return z;
            }
        }
        return null;
    }

    /**
     * Qui peut saisir les résultats : le ministère toujours. Pour un examen organisé par une zone (commune,
     * préfecture, région…), un compte de zone seulement si cette zone organisatrice est la sienne ou est placée sous
     * la sienne : un compte communal ne saisit donc pas un examen régional. Pour un examen national, la saisie reste
     * ouverte à tout compte dont le périmètre couvre l'établissement (le contrôle de périmètre est fait en amont).
     */
    static boolean peutSaisir(Etablissement e, String niveauOrganisation,
                              java.util.Collection<com.plateforme.scolaire.security.UtilisateurPrincipal.AffectationInfo> affectations) {
        if (affectations.stream().anyMatch(a -> a.role() == com.plateforme.scolaire.domain.Role.MINISTERE)) {
            return true;
        }
        if (niveauOrganisation == null || NATIONAL.equalsIgnoreCase(niveauOrganisation)) {
            return true;
        }
        ZoneGeographique organisatrice = zoneOrganisatrice(e, niveauOrganisation);
        if (organisatrice == null) {
            return false;
        }
        for (ZoneGeographique z = organisatrice; z != null; z = z.getParent()) {
            for (var a : affectations) {
                if (a.role() == com.plateforme.scolaire.domain.Role.DIRECTION_REGIONALE && a.zoneId() != null
                        && a.zoneId().equals(z.getId())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Transactional(readOnly = true)
    public List<ExamenDto> lister(Long anneeScolaireId) {
        return examenRepository.findByAnneeScolaireIdOrderByLibelle(anneeScolaireId).stream().map(ExamenDto::de).toList();
    }

    public ExamenDto creer(ExamenRequest r) {
        var annee = anneeRepository.findById(r.anneeScolaireId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Année scolaire inconnue"));
        String libelle = r.libelle().trim();
        if (examenRepository.existsByLibelleIgnoreCaseAndAnneeScolaireId(libelle, annee.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cet examen existe déjà pour cette année");
        }
        CycleScolaire cycle = null;
        if (r.cycleId() != null) {
            cycle = cycleRepository.findById(r.cycleId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cycle inconnu"));
        }
        Niveau niveau = null;
        if (r.niveauId() != null) {
            niveau = niveauRepository.findById(r.niveauId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Niveau inconnu"));
            if (cycle == null) {
                cycle = niveau.getCycle();
            }
        }
        String organisation = r.niveauOrganisation() == null || r.niveauOrganisation().isBlank()
                ? NATIONAL : r.niveauOrganisation().trim();
        if (!niveauxOrganisation().stream().anyMatch(n -> n.equalsIgnoreCase(organisation))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Niveau d'organisation inconnu : NATIONAL ou un type de zone existant");
        }
        verifierPeriode(r.dateDebut(), r.dateFin());
        ExamenNational e = new ExamenNational();
        e.setDateDebut(r.dateDebut());
        e.setDateFin(r.dateFin());
        e.setNiveau(niveau);
        e.setNiveauOrganisation(NATIONAL.equalsIgnoreCase(organisation) ? NATIONAL : niveauxOrganisation().stream()
                .filter(n -> n.equalsIgnoreCase(organisation)).findFirst().orElse(organisation));
        if (r.typesEtablissementIds() != null && !r.typesEtablissementIds().isEmpty()) {
            var ids = new java.util.LinkedHashSet<>(r.typesEtablissementIds());
            var types = typeRepository.findAllById(ids);
            if (types.size() != ids.size()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Type d'établissement inconnu");
            }
            e.getTypesEtablissement().addAll(types);
        }
        e.setLibelle(libelle);
        e.setAnneeScolaire(annee);
        e.setCycle(cycle);
        ExamenNational enregistre = examenRepository.save(e);
        auditService.enregistrer("EXAMEN_CREE", "ExamenNational", enregistre.getId(), Map.of("libelle", libelle));
        return ExamenDto.de(enregistre);
    }

    static void verifierPeriode(java.time.LocalDate debut, java.time.LocalDate fin) {
        if (debut != null && fin != null && fin.isBefore(debut)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fin de la période précède son début");
        }
    }

    /** Qui gère le calendrier d'un examen : le ministère ; une direction régionale seulement hors examen national. */
    static boolean peutGererCalendrier(ExamenNational examen,
                                       java.util.Collection<com.plateforme.scolaire.security.UtilisateurPrincipal.AffectationInfo> affectations) {
        if (affectations.stream().anyMatch(a -> a.role() == com.plateforme.scolaire.domain.Role.MINISTERE)) {
            return true;
        }
        boolean national = examen.getNiveauOrganisation() == null || NATIONAL.equalsIgnoreCase(examen.getNiveauOrganisation());
        return !national && affectations.stream().anyMatch(a -> a.role() == com.plateforme.scolaire.domain.Role.DIRECTION_REGIONALE);
    }

    void verifierGestionCalendrier(ExamenNational examen) {
        var principal = perimetreService.utilisateurCourant();
        if (principal != null && !peutGererCalendrier(examen, principal.getAffectations())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Le calendrier de cet examen est géré par son organisateur");
        }
    }

    /** Publie ou corrige la période officielle. */
    public ExamenDto definirPeriode(Long examenId, com.plateforme.scolaire.web.dto.PeriodeExamenRequest r) {
        ExamenNational examen = examen(examenId);
        verifierGestionCalendrier(examen);
        verifierPeriode(r.dateDebut(), r.dateFin());
        examen.setDateDebut(r.dateDebut());
        examen.setDateFin(r.dateFin());
        auditService.enregistrer("EXAMEN_PERIODE_DEFINIE", "ExamenNational", examenId,
                Map.of("debut", String.valueOf(r.dateDebut()), "fin", String.valueOf(r.dateFin())));
        return ExamenDto.de(examen);
    }

    /**
     * Supprime un examen planifié avec son calendrier et ses centres. Si des résultats ou des candidats sont déjà
     * enregistrés, il faut le confirmer (forcer) : ils sont supprimés avec l'examen.
     */
    public void supprimer(Long examenId, boolean forcer) {
        ExamenNational examen = examen(examenId);
        verifierGestionCalendrier(examen);
        long resultats = examenRepository.nombreResultats(examenId);
        long candidats = examenRepository.nombreCandidats(examenId);
        if (!forcer && (resultats > 0 || candidats > 0)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cet examen contient déjà " + candidats + " candidat(s) et " + resultats
                            + " résultat(s) d'établissement : ils seront supprimés avec lui.");
        }
        String libelle = examen.getLibelle();
        examenRepository.delete(examen);
        examenRepository.flush();
        auditService.enregistrer("EXAMEN_SUPPRIME", "ExamenNational", examenId,
                Map.of("libelle", libelle, "candidats", candidats, "resultats", resultats));
    }

    /** Saisit ou corrige les résultats d'un établissement (l'accès à l'établissement est contrôlé par le contrôleur). */
    public void saisir(Long examenId, Long etablissementId, ResultatExamenRequest r) {
        ExamenNational examen = examen(examenId);
        Etablissement etablissement = etablissementRepository.findById(etablissementId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Établissement introuvable"));
        if ("PROCLAME".equals(examen.getStatutResultats())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Les résultats de cet examen ont été proclamés : la saisie est close");
        }
        var principal = perimetreService.utilisateurCourant();
        if (principal != null && !peutSaisir(etablissement, examen.getNiveauOrganisation(), principal.getAffectations())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Les résultats de cet examen sont saisis par la zone qui l'organise (ou une zone au-dessus)");
        }
        if (r.admis() > r.presentes()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Les admis ne peuvent pas dépasser les présentés");
        }
        verifierDetails(r);
        if (examen.getCycle() != null && etablissement.getCycles().stream()
                .noneMatch(c -> c.getId().equals(examen.getCycle().getId()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cet établissement ne propose pas le cycle de l'examen (" + examen.getCycle().getLibelle() + ")");
        }
        ResultatExamen resultat = resultatRepository.findByExamenIdAndEtablissementId(examenId, etablissementId)
                .orElseGet(() -> {
                    ResultatExamen nouveau = new ResultatExamen();
                    nouveau.setExamen(examen);
                    nouveau.setEtablissement(etablissement);
                    return nouveau;
                });
        resultat.setPresentes(r.presentes());
        resultat.setAdmis(r.admis());
        appliquerDetails(resultat, r);
        resultatRepository.save(resultat);
        auditService.enregistrer("RESULTAT_EXAMEN_SAISI", "ResultatExamen", resultat.getId(),
                Map.of("examen", examen.getLibelle(), "etablissement", etablissement.getNom()));
    }

    static final java.util.List<String> SERIES = java.util.List.of("SCIENCES_MATHEMATIQUES", "SCIENCES_SOCIALES", "SCIENCES_EXPERIMENTALES");

    /** Cohérence des informations détaillées : filles ≤ présents, séries = total des présents et des admis. */
    static void verifierDetails(ResultatExamenRequest r) {
        if ((r.presentesFilles() == null) != (r.admisFilles() == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indiquez à la fois les filles présentes et les filles admises");
        }
        if (r.presentesFilles() != null) {
            if (r.presentesFilles() > r.presentes()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Les filles présentes ne peuvent pas dépasser les présentés");
            }
            if (r.admisFilles() > r.presentesFilles() || r.admisFilles() > r.admis()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Les filles admises sont plus nombreuses que les filles présentes ou que les admis");
            }
            if (r.admis() - r.admisFilles() > r.presentes() - r.presentesFilles()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Les garçons admis dépasseraient les garçons présents");
            }
        }
        if (r.series() != null && !r.series().isEmpty()) {
            long presentes = 0;
            long admis = 0;
            java.util.Set<String> vues = new java.util.HashSet<>();
            for (var serie : r.series()) {
                if (!SERIES.contains(serie.serie()) || !vues.add(serie.serie())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Série inconnue ou répétée : " + serie.serie());
                }
                if (serie.admis() > serie.presentes()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dans une série, les admis dépassent les présentés");
                }
                presentes += serie.presentes();
                admis += serie.admis();
            }
            if (presentes != r.presentes() || admis != r.admis()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Le total des séries (" + presentes + " présentés, " + admis + " admis) doit égaler les résultats de l'établissement");
            }
        }
    }

    private static void appliquerDetails(ResultatExamen resultat, ResultatExamenRequest r) {
        if (r.absents() == null && r.elimines() == null && r.presentesFilles() == null && r.admisFilles() == null
                && (r.series() == null || r.series().isEmpty())) {
            return;   // saisie simple (ou import) : les détails déjà enregistrés sont conservés
        }
        resultat.setAbsents(r.absents() == null ? 0 : r.absents());
        resultat.setElimines(r.elimines() == null ? 0 : r.elimines());
        resultat.setPresentesFilles(r.presentesFilles());
        resultat.setAdmisFilles(r.admisFilles());
        resultat.getSeries().clear();
        if (r.series() != null) {
            r.series().forEach(x -> resultat.getSeries().add(new ResultatExamen.SerieResultat(x.serie(), x.presentes(), x.admis())));
        }
    }

    @Transactional(readOnly = true)
    public RapportExamenDto nationale(Long examenId, Long zoneId) {
        List<Etablissement> etablissements = zoneId == null
                ? etablissementRepository.findAll()
                : etablissementRepository.findByZoneEtSousZones(zoneId);
        return rapport(examenId, etablissements);
    }

    @Transactional(readOnly = true)
    public RapportExamenDto regionale(Long examenId) {
        return rapport(examenId, new ArrayList<>(regionService.etablissementsDeLaRegion().values()));
    }

    private RapportExamenDto rapport(Long examenId, List<Etablissement> tous) {
        ExamenNational examen = examen(examenId);
        List<Etablissement> concernes = tous.stream()
                .filter(e -> examen.getCycle() == null
                        || e.getCycles().stream().anyMatch(c -> c.getId().equals(examen.getCycle().getId())))
                .filter(e -> examen.accepteType(e.getTypeEtablissement()))
                .sorted(Comparator.comparing(Etablissement::getNom, String.CASE_INSENSITIVE_ORDER))
                .toList();
        Map<Long, int[]> chiffres = new HashMap<>();
        Map<Long, ResultatExamen> details = new HashMap<>();
        if (!concernes.isEmpty()) {
            for (ResultatExamen r : resultatRepository.deLExamen(examenId, concernes.stream().map(e -> e.getId()).toList())) {
                chiffres.put(r.getEtablissement().getId(), new int[] {r.getPresentes(), r.getAdmis()});
                details.put(r.getEtablissement().getId(), r);
            }
        }
        List<RapportExamenDto.Ligne> lignes = new ArrayList<>();
        for (Etablissement e : concernes) {
            int[] c = chiffres.get(e.getId());
            ResultatExamen d = details.get(e.getId());
            lignes.add(new RapportExamenDto.Ligne(e.getId(), e.getCodeOfficiel(), e.getNom(), e.getSecteur(),
                    e.getZone() == null ? null : e.getZone().getId(), e.getZone() == null ? null : e.getZone().getNom(),
                    c == null ? null : c[0], c == null ? null : c[1], c == null ? null : taux(c[1], c[0]),
                    organisateur(e, examen.getNiveauOrganisation()),
                    d == null ? null : d.getAbsents(), d == null ? null : d.getElimines(),
                    d == null ? null : d.getPresentesFilles(), d == null ? null : d.getAdmisFilles(),
                    d == null ? List.of() : d.getSeries().stream()
                            .map(x -> new ResultatExamenRequest.Serie(x.getSerie(), x.getPresentes(), x.getAdmis())).toList()));
        }
        return new RapportExamenDto(ExamenDto.de(examen), synthese("Total", lignes),
                List.of(synthese("PUBLIC", lignes.stream().filter(l -> "PUBLIC".equals(l.secteur())).toList()),
                        synthese("PRIVE", lignes.stream().filter(l -> "PRIVE".equals(l.secteur())).toList())),
                lignes, statistiques(lignes, concernes));
    }

    /** Absents, éliminés, taux par sexe, par région, par préfecture et par série, d'après les lignes transmises. */
    static RapportExamenDto.Statistiques statistiques(List<RapportExamenDto.Ligne> lignes, List<Etablissement> etablissements) {
        long absents = lignes.stream().filter(l -> l.absents() != null).mapToLong(RapportExamenDto.Ligne::absents).sum();
        long elimines = lignes.stream().filter(l -> l.elimines() != null).mapToLong(RapportExamenDto.Ligne::elimines).sum();
        long fillesP = 0;
        long fillesA = 0;
        long garconsP = 0;
        long garconsA = 0;
        Map<String, long[]> series = new java.util.TreeMap<>();
        for (RapportExamenDto.Ligne l : lignes) {
            if (l.presentesFilles() != null && l.admisFilles() != null && l.presentes() != null && l.admis() != null) {
                fillesP += l.presentesFilles();
                fillesA += l.admisFilles();
                garconsP += l.presentes() - l.presentesFilles();
                garconsA += l.admis() - l.admisFilles();
            }
            for (var s : l.series()) {
                long[] t = series.computeIfAbsent(s.serie(), k -> new long[2]);
                t[0] += s.presentes();
                t[1] += s.admis();
            }
        }
        Map<Long, Etablissement> parId = new HashMap<>();
        etablissements.forEach(e -> parId.put(e.getId(), e));
        return new RapportExamenDto.Statistiques(absents, elimines,
                List.of(new RapportExamenDto.Repartition("Filles", fillesP, fillesA, taux(fillesA, fillesP)),
                        new RapportExamenDto.Repartition("Garçons", garconsP, garconsA, taux(garconsA, garconsP))),
                regrouper(lignes, parId, "REGION"), regrouper(lignes, parId, "PREFECTURE"),
                series.entrySet().stream().map(e -> new RapportExamenDto.Repartition(e.getKey(), e.getValue()[0], e.getValue()[1],
                        taux(e.getValue()[1], e.getValue()[0]))).toList());
    }

    /** Synthèse par zone ancêtre du type demandé (région, préfecture) ; les établissements sans cette zone sont omis. */
    private static List<RapportExamenDto.Synthese> regrouper(List<RapportExamenDto.Ligne> lignes, Map<Long, Etablissement> parId,
                                                              String typeZone) {
        Map<String, List<RapportExamenDto.Ligne>> groupes = new java.util.TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (RapportExamenDto.Ligne l : lignes) {
            Etablissement e = parId.get(l.id());
            var z = e == null ? null : e.getZone();
            while (z != null && !typeZone.equalsIgnoreCase(z.getTypeZone())) {
                z = z.getParent();
            }
            if (z != null) {
                groupes.computeIfAbsent(z.getNom(), k -> new ArrayList<>()).add(l);
            }
        }
        return groupes.entrySet().stream().map(e -> synthese(e.getKey(), e.getValue())).toList();
    }

    static RapportExamenDto.Synthese synthese(String libelle, List<RapportExamenDto.Ligne> lignes) {
        long transmis = lignes.stream().filter(l -> l.presentes() != null).count();
        long presentes = lignes.stream().filter(l -> l.presentes() != null).mapToLong(RapportExamenDto.Ligne::presentes).sum();
        long admis = lignes.stream().filter(l -> l.admis() != null).mapToLong(RapportExamenDto.Ligne::admis).sum();
        return new RapportExamenDto.Synthese(libelle, lignes.size(), transmis, presentes, admis, taux(admis, presentes));
    }

    /** Pourcentage d'admis à une décimale ; null quand personne ne s'est présenté. */
    static BigDecimal taux(long admis, long presentes) {
        if (presentes <= 0) {
            return null;
        }
        return BigDecimal.valueOf(admis * 100).divide(BigDecimal.valueOf(presentes), 1, RoundingMode.HALF_UP);
    }

    private ExamenNational examen(Long id) {
        return examenRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Examen introuvable"));
    }
}
