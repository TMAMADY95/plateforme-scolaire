package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.CandidatExamen;
import com.plateforme.scolaire.domain.CentreExamen;
import com.plateforme.scolaire.domain.Etablissement;
import com.plateforme.scolaire.domain.ExamenNational;
import com.plateforme.scolaire.domain.Inscription;
import com.plateforme.scolaire.repository.CandidatExamenRepository;
import com.plateforme.scolaire.repository.CentreExamenRepository;
import com.plateforme.scolaire.repository.EtablissementRepository;
import com.plateforme.scolaire.repository.ExamenNationalRepository;
import com.plateforme.scolaire.repository.InscriptionRepository;
import com.plateforme.scolaire.repository.ZoneGeographiqueRepository;
import com.plateforme.scolaire.security.PerimetreService;
import com.plateforme.scolaire.domain.ZoneGeographique;
import com.plateforme.scolaire.web.dto.AffectationAutomatiqueDto;
import com.plateforme.scolaire.web.dto.CandidatDto;
import com.plateforme.scolaire.web.dto.CentreExamenDto;
import com.plateforme.scolaire.web.dto.CentreExamenRequest;
import com.plateforme.scolaire.web.dto.ElevePossibleDto;
import com.plateforme.scolaire.web.dto.ExamenDto;
import com.plateforme.scolaire.web.dto.ExamenEtablissementDto;
import com.plateforme.scolaire.web.dto.PreparationExamenDto;
import com.plateforme.scolaire.web.dto.RepartitionCentreDto;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Examens vus par un établissement : il inscrit ses élèves comme candidats (seulement ceux des classes concernées),
 * puis imprime les convocations. Les centres d'examen sont créés par le ministère ou la direction régionale et
 * attribués aux candidats d'un établissement.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ExamenEtablissementService {

    private final ExamenNationalRepository examens;
    private final CandidatExamenRepository candidats;
    private final CentreExamenRepository centres;
    private final InscriptionRepository inscriptions;
    private final EtablissementRepository etablissements;
    private final ZoneGeographiqueRepository zones;
    private final PerimetreService perimetre;
    private final com.plateforme.scolaire.repository.EpreuveExamenRepository epreuves;
    private final GenerateurPdf pdf;
    private final AuditService audit;
    private final Clock clock;
    private final CentresExamenService conditionsCentre;
    private final com.plateforme.scolaire.repository.PresenceRepository presences;
    private final com.plateforme.scolaire.repository.ListeCandidatsRepository listes;

    /** Règlement général : un candidat scolaire doit avoir moins de 25 % d'absences durant l'année. */
    static final int SEUIL_ABSENCES = 25;

    /** Règlement général : deux surveillants par salle. */
    static final int SURVEILLANTS_PAR_SALLE = 2;

    // ---------- Côté établissement ----------

    @Transactional(readOnly = true)
    public List<ExamenEtablissementDto> examensDe(Long etablissementId, Long anneeId) {
        Etablissement etab = etablissement(etablissementId);
        return examens.findByAnneeScolaireIdOrderByLibelle(anneeId).stream()
                .filter(e -> concerne(etab, e))
                .map(e -> new ExamenEtablissementDto(ExamenDto.de(e), candidats.countByExamenIdAndEtablissementId(e.getId(), etablissementId)))
                .toList();
    }

    @Transactional(readOnly = true)
    public PreparationExamenDto preparation(Long etablissementId, Long examenId) {
        ExamenNational examen = examen(examenId);
        List<CandidatExamen> inscrits = candidats.deLEtablissement(examenId, etablissementId);
        Set<Long> dejaCandidats = new HashSet<>();
        inscrits.forEach(c -> dejaCandidats.add(c.getEleve().getId()));
        List<Inscription> concernes = elevesConcernes(etablissementId, examen).stream()
                .filter(i -> !dejaCandidats.contains(i.getEleve().getId()))
                .toList();
        Map<Long, long[]> absences = absences(concernes);
        List<ElevePossibleDto> possibles = concernes.stream()
                .map(i -> {
                    long[] a = absences.get(i.getId());
                    Integer taux = a == null ? null : (int) Math.round(a[1] * 100.0 / a[0]);
                    boolean trop = depasseSeuil(a);
                    return new ElevePossibleDto(i.getEleve().getId(), (i.getEleve().getPrenom() + " " + i.getEleve().getNom()).trim(),
                            i.getEleve().getMatricule(), i.getClasse().getNom(), taux, !trop,
                            trop ? "Au moins " + SEUIL_ABSENCES + " % d'absences durant l'année" : null);
                })
                .toList();
        return new PreparationExamenDto(ExamenDto.de(examen), inscrits.stream().map(CandidatDto::de).toList(), possibles);
    }

    public PreparationExamenDto inscrire(Long etablissementId, Long examenId, List<Long> eleveIds) {
        ExamenNational examen = examen(examenId);
        Etablissement etab = etablissement(etablissementId);
        verifierListeModifiable(examenId, etablissementId);
        Map<Long, Inscription> eligibles = new java.util.HashMap<>();
        elevesConcernes(etablissementId, examen).forEach(i -> eligibles.put(i.getEleve().getId(), i));
        Set<Long> deja = candidats.elevesDejaInscrits(examenId, eleveIds);
        Map<Long, long[]> absences = absences(eleveIds.stream().map(eligibles::get).filter(java.util.Objects::nonNull).toList());
        int nouveaux = 0;
        for (Long eleveId : new HashSet<>(eleveIds)) {
            Inscription i = eligibles.get(eleveId);
            if (i == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Un des élèves n'est pas inscrit dans une classe concernée par cet examen");
            }
            if (deja.contains(eleveId)) {
                continue;
            }
            if (depasseSeuil(absences.get(i.getId()))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, (i.getEleve().getPrenom() + " " + i.getEleve().getNom()).trim()
                        + " totalise au moins " + SEUIL_ABSENCES + " % d'absences : le règlement général des examens interdit sa candidature");
            }
            CandidatExamen c = new CandidatExamen();
            c.setExamen(examen);
            c.setEleve(i.getEleve());
            c.setEtablissement(etab);
            c.setClasse(i.getClasse());
            c.setCreatedAt(Instant.now(clock));
            CandidatExamen enregistre = candidats.save(c);
            enregistre.setNumero(numero(examen, enregistre.getId()));
            nouveaux++;
        }
        if (nouveaux > 0) {
            audit.enregistrer("CANDIDATS_INSCRITS", "ExamenNational", examenId,
                    Map.of("etablissement", etab.getNom(), "nombre", nouveaux));
        }
        return preparation(etablissementId, examenId);
    }

    public void retirer(Long candidatId) {
        CandidatExamen c = candidats.findById(candidatId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Candidat introuvable"));
        if (!perimetre.peutGererEtablissement(c.getEtablissement().getId())) {
            throw new AccessDeniedException("Candidat hors de votre établissement");
        }
        verifierListeModifiable(c.getExamen().getId(), c.getEtablissement().getId());
        candidats.delete(c);
        audit.enregistrer("CANDIDAT_RETIRE", "ExamenNational", c.getExamen().getId(), Map.of("eleve", c.getEleve().getId()));
    }

    @Transactional(readOnly = true)
    public byte[] convocations(Long etablissementId, Long examenId) {
        ExamenNational examen = examen(examenId);
        Etablissement etab = etablissement(etablissementId);
        List<GenerateurPdf.Convocation> liste = candidats.deLEtablissement(examenId, etablissementId).stream()
                .map(c -> new GenerateurPdf.Convocation(
                        (c.getEleve().getPrenom() + " " + c.getEleve().getNom()).trim(), c.getEleve().getMatricule(), c.getNumero(),
                        c.getClasse() == null ? null : c.getClasse().getNom(),
                        c.getCentre() == null ? null : c.getCentre().getNom(),
                        c.getCentre() == null ? null : c.getCentre().getAdresse(),
                        examen.getDateDebut() != null ? examen.getDateDebut().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                                : c.getCentre() == null || c.getCentre().getDateDebut() == null ? null
                                : c.getCentre().getDateDebut().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                        c.getNumeroPv(), c.getSalle(), c.getTableNumero()))
                .toList();
        List<List<String>> calendrier = epreuves.findByExamenIdOrderByDateEpreuveAscHeureDebutAsc(examenId).stream()
                .map(e -> List.of(e.getDateEpreuve().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                        e.getHeureDebut() + " - " + e.getHeureFin(), e.nom()))
                .toList();
        String periode = examen.getDateDebut() == null ? null
                : "Du " + examen.getDateDebut().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                + (examen.getDateFin() == null ? "" : " au " + examen.getDateFin().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        String organisateur = ExamenService.organisateur(etab, examen.getNiveauOrganisation());
        return pdf.convocations(new GenerateurPdf.DonneesConvocations(etab.getNom(), examen.getLibelle(),
                examen.getAnneeScolaire().getLibelle(),
                organisateur != null ? examen.getNiveauOrganisation().toLowerCase() + " de " + organisateur : null, liste,
                periode, calendrier));
    }

    // ---------- Centres (ministère et directions régionales) ----------

    @Transactional(readOnly = true)
    public List<CentreExamenDto> centres(Long examenId) {
        examen(examenId);
        return centres.deLExamen(examenId).stream().map(CentreExamenDto::de).toList();
    }

    public CentreExamenDto creerCentre(Long examenId, CentreExamenRequest r) {
        ExamenNational examen = examen(examenId);
        Etablissement siege = r.etablissementId() == null ? null : etablissements.findById(r.etablissementId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Établissement inconnu"));
        if (siege == null && (r.nom() == null || r.nom().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indiquez un établissement ou le nom du centre");
        }
        if (siege != null) {
            conditionsCentre.verifierEligible(siege);
        }
        String nom = (r.nom() == null || r.nom().isBlank() ? siege.getNom() : r.nom()).trim();
        if (centres.existsByExamenIdAndNomIgnoreCase(examenId, nom)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce centre existe déjà pour cet examen");
        }
        CentreExamen c = new CentreExamen();
        c.setExamen(examen);
        c.setEtablissement(siege);
        c.setNom(nom);
        c.setAdresse(r.adresse() == null || r.adresse().isBlank() ? (siege == null ? null : siege.getAdresse()) : r.adresse().trim());
        c.setDateDebut(r.dateDebut());
        if (r.zoneId() == null && siege != null) {
            c.setZone(siege.getZone());
        } else if (r.zoneId() != null) {
            c.setZone(zones.findById(r.zoneId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Zone inconnue")));
        }
        c.setCreatedAt(Instant.now(clock));
        CentreExamen enregistre = centres.save(c);
        audit.enregistrer("CENTRE_EXAMEN_CREE", "CentreExamen", enregistre.getId(), Map.of("nom", nom));
        return CentreExamenDto.de(enregistre);
    }

    public void supprimerCentre(Long centreId) {
        CentreExamen c = centres.findById(centreId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Centre introuvable"));
        centres.delete(c);
    }

    /** Attribue un centre à tous les candidats d'un établissement (l'accès à l'établissement est vérifié par le contrôleur). */
    public int affecterCentre(Long examenId, Long etablissementId, Long centreId) {
        CentreExamen centre = centres.findById(centreId)
                .filter(c -> c.getExamen().getId().equals(examenId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Centre inconnu pour cet examen"));
        List<CandidatExamen> liste = candidats.deLEtablissement(examenId, etablissementId);
        var capacite = conditionsCentre.capaciteCandidats(centre.getEtablissement());
        if (capacite != null && capacite.isPresent()) {
            long dejaLa = candidats.countByCentreId(centre.getId())
                    - liste.stream().filter(c -> c.getCentre() != null && c.getCentre().getId().equals(centreId)).count();
            if (dejaLa + liste.size() > capacite.get()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, centre.getNom() + " ne peut accueillir que "
                        + capacite.get() + " candidats (" + CentresExamenService.CANDIDATS_PAR_SALLE + " par salle au maximum) : "
                        + dejaLa + " y sont déjà affectés");
            }
        }
        liste.forEach(c -> {
            if (c.getCentre() == null || !c.getCentre().getId().equals(centreId)) {
                placerDans(c, centre);
            }
        });
        audit.enregistrer("CENTRE_EXAMEN_AFFECTE", "CentreExamen", centreId,
                Map.of("etablissement", etablissementId, "candidats", liste.size()));
        return liste.size();
    }

    /**
     * Place dans un centre de leur commune les candidats qui n'en ont pas encore : tous les candidats d'un même
     * établissement vont dans le même centre, le moins chargé quand la commune en compte plusieurs. Les affectations
     * déjà faites (à la main) ne sont jamais modifiées.
     */
    public AffectationAutomatiqueDto affecterAutomatiquement(Long examenId) {
        examen(examenId);
        List<CentreExamen> tous = centres.deLExamen(examenId);
        Map<Long, Long> charge = new java.util.HashMap<>();
        Map<Long, Long> capacites = new java.util.HashMap<>();
        for (CentreExamen c : tous) {
            charge.put(c.getId(), candidats.countByCentreId(c.getId()));
            var cap = conditionsCentre.capaciteCandidats(c.getEtablissement());
            if (cap != null && cap.isPresent()) {
                capacites.put(c.getId(), cap.get());
            }
        }
        Map<Long, List<CandidatExamen>> parEtablissement = new java.util.LinkedHashMap<>();
        candidats.sansCentre(examenId).forEach(c -> parEtablissement.computeIfAbsent(c.getEtablissement().getId(),
                k -> new java.util.ArrayList<>()).add(c));
        int affectes = 0;
        int etabs = 0;
        List<String> sansCentre = new java.util.ArrayList<>();
        for (List<CandidatExamen> groupe : parEtablissement.values()) {
            Etablissement etab = groupe.get(0).getEtablissement();
            CentreExamen choisi = centreDeLaCommune(etab, tous, charge, capacites, groupe.size());
            if (choisi == null) {
                boolean complets = centreDeLaCommune(etab, tous, charge) != null;
                sansCentre.add(complets ? etab.getNom() + " (centres de la commune complets)" : etab.getNom());
                continue;
            }
            groupe.forEach(c -> placerDans(c, choisi));
            charge.merge(choisi.getId(), (long) groupe.size(), Long::sum);
            affectes += groupe.size();
            etabs++;
        }
        audit.enregistrer("CENTRES_AFFECTES_AUTOMATIQUEMENT", "ExamenNational", examenId,
                Map.of("candidats", affectes, "sansCentre", sansCentre.size()));
        return new AffectationAutomatiqueDto(affectes, etabs, sansCentre);
    }

    /** La commune d'une zone : la zone de type COMMUNE au-dessus (ou égale), sinon la zone elle-même. */
    static ZoneGeographique commune(ZoneGeographique zone) {
        for (ZoneGeographique z = zone; z != null; z = z.getParent()) {
            if ("COMMUNE".equalsIgnoreCase(z.getTypeZone())) {
                return z;
            }
        }
        return zone;
    }

    /** Le centre de la même commune que l'établissement, le moins chargé ; null s'il n'y en a pas. */
    static CentreExamen centreDeLaCommune(Etablissement etab, List<CentreExamen> centres, Map<Long, Long> charge) {
        return centreDeLaCommune(etab, centres, charge, Map.of(), 0);
    }

    /** Idem, en ne gardant que les centres où {@code nouveaux} candidats tiennent encore (30 par salle au plus). */
    static CentreExamen centreDeLaCommune(Etablissement etab, List<CentreExamen> centres, Map<Long, Long> charge,
                                          Map<Long, Long> capacites, int nouveaux) {
        ZoneGeographique commune = commune(etab.getZone());
        if (commune == null) {
            return null;
        }
        CentreExamen meilleur = null;
        for (CentreExamen c : centres) {
            ZoneGeographique zoneCentre = commune(c.getEtablissement() != null ? c.getEtablissement().getZone() : c.getZone());
            if (zoneCentre == null || !commune.getId().equals(zoneCentre.getId())) {
                continue;
            }
            Long capacite = capacites.get(c.getId());
            if (capacite != null && charge.getOrDefault(c.getId(), 0L) + nouveaux > capacite) {
                continue;
            }
            if (meilleur == null || charge.getOrDefault(c.getId(), 0L) < charge.getOrDefault(meilleur.getId(), 0L)) {
                meilleur = c;
            }
        }
        return meilleur;
    }

    // ---------- Répartition en salles et numéros de PV ----------

    /** Plan enregistré d'un centre (vide tant que la répartition n'a pas été générée). */
    @Transactional(readOnly = true)
    public RepartitionCentreDto repartition(Long examenId, Long centreId) {
        CentreExamen centre = centreDeLExamen(examenId, centreId);
        return plan(centre, candidats.duCentre(centreId));
    }

    /**
     * Répartit les candidats du centre par salle de 30 au plus (une table chacun) et leur attribue un numéro de PV
     * unique. Refaire la répartition recalcule tout, par exemple après l'arrivée de nouveaux candidats.
     */
    public RepartitionCentreDto repartir(Long examenId, Long centreId) {
        CentreExamen centre = centreDeLExamen(examenId, centreId);
        List<CandidatExamen> liste = candidats.duCentre(centreId);
        var capacite = conditionsCentre.capaciteCandidats(centre.getEtablissement());
        if (capacite != null && capacite.isPresent() && liste.size() > capacite.get()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, centre.getNom() + " ne peut accueillir que " + capacite.get()
                    + " candidats (" + CentresExamenService.CANDIDATS_PAR_SALLE + " par salle au maximum) : " + liste.size()
                    + " y sont affectés. Déplacez des candidats vers un autre centre.");
        }
        for (int i = 0; i < liste.size(); i++) {
            CandidatExamen c = liste.get(i);
            c.setSalle(i / CentresExamenService.CANDIDATS_PAR_SALLE + 1);
            c.setTableNumero(i % CentresExamenService.CANDIDATS_PAR_SALLE + 1);
            c.setNumeroPv(numeroPv(examenId, centreId, i + 1));
        }
        audit.enregistrer("CENTRE_EXAMEN_REPARTI", "CentreExamen", centreId, Map.of("candidats", liste.size()));
        return plan(centre, liste);
    }

    /** Relevés de notes et attestations de réussite des candidats d'un établissement ; seulement après la proclamation. */
    @Transactional(readOnly = true)
    public byte[] releves(Long etablissementId, Long examenId) {
        ExamenNational examen = examen(examenId);
        Etablissement etab = etablissement(etablissementId);
        if (!"PROCLAME".equals(examen.getStatutResultats())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Les relevés sont délivrés après la proclamation des résultats");
        }
        List<com.plateforme.scolaire.domain.EpreuveExamen> liste = epreuves.findByExamenIdOrderByDateEpreuveAscHeureDebutAsc(examenId);
        List<GenerateurPdf.Releve> releves = candidats.deLEtablissement(examenId, etablissementId).stream()
                .filter(c -> "PRESENT".equals(c.getStatutEpreuves()) || "ELIMINE".equals(c.getStatutEpreuves()))
                .map(c -> new GenerateurPdf.Releve((c.getEleve().getPrenom() + " " + c.getEleve().getNom()).trim(),
                        c.getEleve().getMatricule(), c.getNumeroPv(), c.getStatutEpreuves(),
                        liste.stream().map(x -> new GenerateurPdf.LigneReleve(x.nom(), x.getCoefficient(), c.getNotes().get(x.getId()))).toList(),
                        c.getMoyenne(), c.getDecision()))
                .toList();
        return pdf.releves(new GenerateurPdf.DonneesReleves(etab.getNom(), examen.getLibelle(),
                examen.getAnneeScolaire().getLibelle(),
                examen.getDateProclamation() == null ? null
                        : examen.getDateProclamation().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")), releves));
    }

    /** Procès-verbal d'appel du centre (une page par salle). */
    @Transactional(readOnly = true)
    public byte[] listeAppel(Long examenId, Long centreId) {
        ExamenNational examen = examen(examenId);
        RepartitionCentreDto plan = repartition(examenId, centreId);
        List<GenerateurPdf.SalleAppel> salles = plan.plan().stream()
                .map(s -> new GenerateurPdf.SalleAppel(s.numero(), s.places().stream()
                        .map(p -> List.of(String.valueOf(p.table()), p.numeroPv(), p.nom(), p.etablissement())).toList()))
                .toList();
        return pdf.listeAppel(new GenerateurPdf.DonneesListeAppel(plan.centre(), examen.getLibelle(),
                examen.getAnneeScolaire().getLibelle(), salles));
    }

    static String numeroPv(Long examenId, Long centreId, int rang) {
        return String.format("PV%d-%d-%04d", examenId, centreId, rang);
    }

    private RepartitionCentreDto plan(CentreExamen centre, List<CandidatExamen> liste) {
        Map<Integer, List<RepartitionCentreDto.Place>> parSalle = new java.util.TreeMap<>();
        boolean repartie = !liste.isEmpty() && liste.stream().allMatch(c -> c.getSalle() != null);
        if (repartie) {
            for (CandidatExamen c : liste.stream().sorted(Comparator.comparing(CandidatExamen::getSalle)
                    .thenComparing(CandidatExamen::getTableNumero)).toList()) {
                parSalle.computeIfAbsent(c.getSalle(), k -> new java.util.ArrayList<>()).add(new RepartitionCentreDto.Place(
                        c.getTableNumero(), c.getNumeroPv(),
                        (c.getEleve().getPrenom() + " " + c.getEleve().getNom()).trim(), c.getEleve().getMatricule(),
                        c.getEtablissement().getNom()));
            }
        }
        List<RepartitionCentreDto.Salle> salles = parSalle.entrySet().stream()
                .map(e -> new RepartitionCentreDto.Salle(e.getKey(), e.getValue())).toList();
        int nombreSalles = repartie ? salles.size()
                : (liste.size() + CentresExamenService.CANDIDATS_PAR_SALLE - 1) / CentresExamenService.CANDIDATS_PAR_SALLE;
        var capacite = conditionsCentre.capaciteCandidats(centre.getEtablissement());
        return new RepartitionCentreDto(centre.getId(), centre.getNom(), liste.size(), nombreSalles, nombreSalles * SURVEILLANTS_PAR_SALLE,
                capacite == null ? null : capacite.orElse(null), repartie, salles);
    }

    private CentreExamen centreDeLExamen(Long examenId, Long centreId) {
        return centres.findById(centreId).filter(c -> c.getExamen().getId().equals(examenId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Centre inconnu pour cet examen"));
    }

    /** Efface la place d'un candidat (changement de centre) : la répartition est à refaire. */
    private static void placerDans(CandidatExamen c, CentreExamen centre) {
        c.setCentre(centre);
        c.setSalle(null);
        c.setTableNumero(null);
        c.setNumeroPv(null);
    }

    // ---------- Interne ----------

    /** Par inscription : {séances saisies, absences}. Absent de la table : aucune présence saisie. */
    private Map<Long, long[]> absences(List<Inscription> liste) {
        Map<Long, long[]> resultat = new java.util.HashMap<>();
        if (liste.isEmpty()) {
            return resultat;
        }
        for (Object[] l : presences.absencesParInscription(liste.stream().map(Inscription::getId).toList())) {
            long total = ((Number) l[1]).longValue();
            if (total > 0) {
                resultat.put((Long) l[0], new long[] {total, l[2] == null ? 0 : ((Number) l[2]).longValue()});
            }
        }
        return resultat;
    }

    /** Vrai à partir de 25 % d'absences (« moins de 25 % » est exigé). */
    static boolean depasseSeuil(long[] seancesEtAbsences) {
        return seancesEtAbsences != null && seancesEtAbsences[1] * 100 >= (long) SEUIL_ABSENCES * seancesEtAbsences[0];
    }

    /** Inscriptions acceptées de l'année de l'examen, dans une classe du niveau (ou du cycle) de l'examen. */
    /** Une liste transmise ou validée n'est plus modifiable : il faut d'abord qu'elle soit rejetée. */
    private void verifierListeModifiable(Long examenId, Long etablissementId) {
        if (listes.findByExamenIdAndEtablissementId(examenId, etablissementId).filter(l -> !"BROUILLON".equals(l.getStatut())).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La liste des candidats a été transmise : elle n'est plus modifiable");
        }
    }

    private List<Inscription> elevesConcernes(Long etablissementId, ExamenNational examen) {
        return inscriptions.findByEtablissementIdAndAnneeScolaireIdOrderByDateDemandeDescIdDesc(
                        etablissementId, examen.getAnneeScolaire().getId()).stream()
                .filter(i -> "ACCEPTEE".equals(i.getStatut()) && i.getClasse() != null)
                .filter(i -> classeConcernee(i, examen))
                .sorted(Comparator.comparing((Inscription i) -> i.getEleve().getNom(), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(i -> i.getEleve().getPrenom(), String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private static boolean classeConcernee(Inscription i, ExamenNational examen) {
        var niveau = i.getClasse().getNiveau();
        if (examen.getNiveau() != null) {
            return niveau != null && niveau.getId().equals(examen.getNiveau().getId());
        }
        if (examen.getCycle() != null) {
            return niveau != null && niveau.getCycle() != null && niveau.getCycle().getId().equals(examen.getCycle().getId());
        }
        return true;
    }

    private static boolean concerne(Etablissement etab, ExamenNational examen) {
        return examen.accepteType(etab.getTypeEtablissement())
                && (examen.getCycle() == null
                || etab.getCycles().stream().anyMatch(c -> c.getId().equals(examen.getCycle().getId())));
    }

    static String numero(ExamenNational examen, Long candidatId) {
        return String.format("EX%d-%06d", examen.getId(), candidatId);
    }

    private ExamenNational examen(Long id) {
        return examens.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Examen introuvable"));
    }

    private Etablissement etablissement(Long id) {
        return etablissements.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Établissement introuvable"));
    }
}
