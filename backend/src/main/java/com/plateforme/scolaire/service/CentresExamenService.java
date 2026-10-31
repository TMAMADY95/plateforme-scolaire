package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.CritereCentreExamen;
import com.plateforme.scolaire.domain.Etablissement;
import com.plateforme.scolaire.repository.CritereCentreExamenRepository;
import com.plateforme.scolaire.repository.SalleRepository;
import com.plateforme.scolaire.security.PerimetreService;
import com.plateforme.scolaire.web.dto.CriteresCentreDto;
import com.plateforme.scolaire.web.dto.CriteresCentreRequest;
import com.plateforme.scolaire.web.dto.EtablissementCentreDto;
import java.time.Instant;
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
 * Conditions pour qu'un établissement puisse être centre d'examen (secteur, autorisation d'ouverture, nombre de salles,
 * places d'accueil) et éligibilité de chaque établissement.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CentresExamenService {

    private final CritereCentreExamenRepository criteres;
    private final SalleRepository salles;
    private final PerimetreService perimetre;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public CriteresCentreDto criteres() {
        return CriteresCentreDto.de(courants());
    }

    public CriteresCentreDto modifier(CriteresCentreRequest r) {
        String secteur = r.secteurAdmis().trim().toUpperCase();
        if (!secteur.equals("TOUS") && !secteur.equals("PUBLIC")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Secteur admis : TOUS ou PUBLIC");
        }
        CritereCentreExamen c = courants();
        c.setSecteurAdmis(secteur);
        c.setAutorisationRequise(r.autorisationRequise());
        c.setSallesMinimum(r.sallesMinimum());
        c.setCapaciteMinimum(r.capaciteMinimum());
        c.setUpdatedAt(Instant.now());
        criteres.save(c);
        audit.enregistrer("CRITERES_CENTRES_MODIFIES", "CritereCentreExamen", CritereCentreExamen.ID, Map.of(
                "secteur", secteur, "autorisation", String.valueOf(r.autorisationRequise()),
                "salles", String.valueOf(r.sallesMinimum()), "capacite", String.valueOf(r.capaciteMinimum())));
        return CriteresCentreDto.de(c);
    }

    /** Établissements actifs du périmètre de l'utilisateur, avec leur éligibilité. */
    @Transactional(readOnly = true)
    public List<EtablissementCentreDto> etablissements() {
        CritereCentreExamen c = courants();
        Map<Long, long[]> totaux = new HashMap<>();
        for (Object[] l : salles.totauxParEtablissement()) {
            totaux.put((Long) l[0], new long[] {((Number) l[1]).longValue(), ((Number) l[2]).longValue()});
        }
        return perimetre.etablissementsAccessibles(perimetre.utilisateurCourant()).stream()
                .filter(e -> "ACTIF".equals(e.getStatut()))
                .sorted(Comparator.comparing(Etablissement::getNom, String.CASE_INSENSITIVE_ORDER))
                .map(e -> {
                    long[] t = totaux.getOrDefault(e.getId(), new long[] {0, 0});
                    List<String> motifs = motifs(e.getSecteur(), e.getAutorisationStatut(), t[0], t[1], c);
                    return new EtablissementCentreDto(e.getId(), e.getCodeOfficiel(), e.getNom(),
                            e.getZone() == null ? null : e.getZone().getId(), e.getZone() == null ? null : e.getZone().getNom(),
                            e.getSecteur(), e.getTypeEtablissement() == null ? null : e.getTypeEtablissement().getLibelle(),
                            t[0], t[1], motifs.isEmpty(), motifs);
                })
                .toList();
    }

    /** Règlement général : au plus 30 candidats par salle, un candidat par table-banc. */
    public static final int CANDIDATS_PAR_SALLE = 30;

    /**
     * Nombre maximal de candidats qu'un centre installé dans cet établissement peut accueillir : 30 par salle, dans la
     * limite des places. Vide quand les salles ne sont pas renseignées (capacité inconnue, donc non limitée).
     */
    @Transactional(readOnly = true)
    public java.util.Optional<Long> capaciteCandidats(Etablissement siege) {
        if (siege == null) {
            return java.util.Optional.empty();
        }
        List<Object[]> l = salles.totaux(siege.getId());
        long nombre = l.isEmpty() ? 0 : ((Number) l.get(0)[0]).longValue();
        long places = l.isEmpty() ? 0 : ((Number) l.get(0)[1]).longValue();
        if (nombre <= 0) {
            return java.util.Optional.empty();
        }
        long parSalles = nombre * CANDIDATS_PAR_SALLE;
        return java.util.Optional.of(places > 0 ? Math.min(parSalles, places) : parSalles);
    }

    /** Refuse (409) la désignation d'un établissement qui ne remplit pas les conditions. */
    @Transactional(readOnly = true)
    public void verifierEligible(Etablissement e) {
        List<Object[]> l = salles.totaux(e.getId());
        long nombre = l.isEmpty() ? 0 : ((Number) l.get(0)[0]).longValue();
        long capacite = l.isEmpty() ? 0 : ((Number) l.get(0)[1]).longValue();
        List<String> motifs = new ArrayList<>(motifs(e.getSecteur(), e.getAutorisationStatut(), nombre, capacite, courants()));
        if (!"ACTIF".equals(e.getStatut())) {
            motifs.add(0, "Établissement non actif");
        }
        if (!motifs.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    e.getNom() + " ne peut pas être centre d'examen : " + String.join(" ; ", motifs));
        }
    }

    /** Motifs pour lesquels un établissement ne remplit pas les conditions (liste vide : éligible). */
    static List<String> motifs(String secteur, String autorisation, long nombreSalles, long capacite, CritereCentreExamen c) {
        List<String> motifs = new ArrayList<>();
        if ("PUBLIC".equals(c.getSecteurAdmis()) && !"PUBLIC".equals(secteur)) {
            motifs.add("Réservé aux établissements publics");
        }
        if (c.isAutorisationRequise() && !"AUTORISE".equals(autorisation)) {
            motifs.add("Autorisation d'ouverture requise");
        }
        if (nombreSalles < c.getSallesMinimum()) {
            motifs.add("Au moins " + c.getSallesMinimum() + " salle(s) requise(s) (" + nombreSalles + " enregistrée(s))");
        }
        if (capacite < c.getCapaciteMinimum()) {
            motifs.add("Capacité insuffisante (" + capacite + " place(s) sur " + c.getCapaciteMinimum() + " requises)");
        }
        return motifs;
    }

    private CritereCentreExamen courants() {
        return criteres.findById(CritereCentreExamen.ID).orElseGet(CritereCentreExamen::new);
    }
}
