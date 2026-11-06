package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.AbandonScolaire;
import com.plateforme.scolaire.domain.AnneeScolaire;
import com.plateforme.scolaire.domain.Etablissement;
import com.plateforme.scolaire.domain.ProfilVulnerabiliteEleve;
import com.plateforme.scolaire.repository.AbandonScolaireRepository;
import com.plateforme.scolaire.repository.AnneeScolaireRepository;
import com.plateforme.scolaire.repository.EtablissementRepository;
import com.plateforme.scolaire.repository.ProfilVulnerabiliteEleveRepository;
import com.plateforme.scolaire.security.PerimetreService;
import com.plateforme.scolaire.security.UtilisateurPrincipal;
import com.plateforme.scolaire.web.dto.SuiviElevesDto;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Abandon scolaire (motif, date) et vulnérabilités (orphelin, handicap). L'abandon n'enlève pas l'élève des effectifs de l'année : il est
 * compté à part pour calculer un taux d'abandon. Les listes nominatives restent à la direction de l'établissement.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class SuiviElevesService {

    private final AbandonScolaireRepository abandons;
    private final ProfilVulnerabiliteEleveRepository profils;
    private final AnneeScolaireRepository annees;
    private final EtablissementRepository etablissements;
    private final PerimetreService perimetre;
    private final EntityManager em;
    private final AuditService audit;
    private final Clock clock;

    /** Pur : abandons pour 100 élèves, une décimale ; null sans élève. */
    static Double taux(int abandons, int eleves) {
        if (eleves <= 0) {
            return null;
        }
        return BigDecimal.valueOf(abandons * 100L).divide(BigDecimal.valueOf(eleves), 1, RoundingMode.HALF_UP).doubleValue();
    }

    /** Pur : message d'erreur si la date d'abandon sort de l'année scolaire ou est dans le futur ; sinon null. */
    static String verifierDate(LocalDate date, LocalDate debut, LocalDate fin, LocalDate aujourdhui) {
        if (date.isBefore(debut) || date.isAfter(fin)) {
            return "La date d'abandon doit se situer dans l'année scolaire";
        }
        if (date.isAfter(aujourdhui)) {
            return "La date d'abandon ne peut pas être dans le futur";
        }
        return null;
    }

    private static int entier(Object o) {
        return o == null ? 0 : ((Number) o).intValue();
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public SuiviElevesDto.Suivi suivi(Long etablissementId, Long anneeId) {
        if (!etablissements.existsById(etablissementId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Établissement introuvable");
        }
        if (!annees.existsById(anneeId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Année scolaire introuvable");
        }
        List<Object[]> lignes = em.createNativeQuery("SELECT i.id, e.id, e.matricule, e.nom || ' ' || e.prenom, c.nom, e.sexe FROM inscription i "
                + "JOIN eleve e ON e.id = i.eleve_id LEFT JOIN classe c ON c.id = i.classe_id WHERE i.etablissement_id = :e AND i.annee_scolaire_id = :a "
                + "AND i.statut = 'ACCEPTEE' ORDER BY c.nom, e.nom, e.prenom").setParameter("e", etablissementId).setParameter("a", anneeId).getResultList();
        Map<Long, AbandonScolaire> parInscription = new HashMap<>();
        abandons.findByEtablissementIdAndAnneeScolaireId(etablissementId, anneeId).forEach(a -> parInscription.put(a.getInscriptionId(), a));
        Map<Long, ProfilVulnerabiliteEleve> parEleve = new HashMap<>();
        if (!lignes.isEmpty()) {
            profils.findByEleveIdIn(lignes.stream().map(r -> ((Number) r[1]).longValue()).toList()).forEach(p -> parEleve.put(p.getEleveId(), p));
        }
        List<SuiviElevesDto.Ligne> res = new ArrayList<>();
        for (Object[] r : lignes) {
            Long inscriptionId = ((Number) r[0]).longValue();
            Long eleveId = ((Number) r[1]).longValue();
            AbandonScolaire a = parInscription.get(inscriptionId);
            ProfilVulnerabiliteEleve p = parEleve.get(eleveId);
            res.add(new SuiviElevesDto.Ligne(inscriptionId, eleveId, (String) r[2], (String) r[3], (String) r[4], r[5] == null ? null : String.valueOf(r[5]),
                    p == null ? "AUCUN" : p.getOrphelin(), p == null ? null : p.getHandicap(),
                    a == null ? null : new SuiviElevesDto.Abandon(a.getId(), a.getDateAbandon(), a.getMotif(), a.getObservation())));
        }
        return new SuiviElevesDto.Suivi(etablissementId, anneeId, res);
    }

    @SuppressWarnings("unchecked")
    public SuiviElevesDto.Suivi declarerAbandon(Long etablissementId, SuiviElevesDto.DeclarationAbandon q, String auteur) {
        List<Object[]> trouvees = em.createNativeQuery("SELECT i.eleve_id, i.annee_scolaire_id FROM inscription i WHERE i.id = :i AND i.etablissement_id = :e "
                + "AND i.statut = 'ACCEPTEE'").setParameter("i", q.inscriptionId()).setParameter("e", etablissementId).getResultList();
        if (trouvees.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Inscription acceptée introuvable dans cet établissement");
        }
        Object[] insc = trouvees.get(0);
        Long eleveId = ((Number) insc[0]).longValue();
        Long anneeId = ((Number) insc[1]).longValue();
        if (abandons.existsByInscriptionId(q.inscriptionId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "L'abandon de cet élève est déjà déclaré");
        }
        AnneeScolaire annee = annees.findById(anneeId).orElseThrow();
        String message = verifierDate(q.dateAbandon(), annee.getDateDebut(), annee.getDateFin(), LocalDate.now(clock));
        if (message != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        AbandonScolaire a = new AbandonScolaire();
        a.setInscriptionId(q.inscriptionId());
        a.setEleveId(eleveId);
        a.setEtablissementId(etablissementId);
        a.setAnneeScolaireId(anneeId);
        a.setDateAbandon(q.dateAbandon());
        a.setMotif(q.motif());
        a.setObservation(q.observation() == null || q.observation().isBlank() ? null : q.observation().trim());
        a.setSignalePar(auteur);
        a.setSignaleLe(Instant.now(clock));
        abandons.save(a);
        audit.enregistrer("ABANDON_SCOLAIRE_DECLARE", "Eleve", eleveId, Map.of("motif", q.motif(), "etablissement", etablissementId));
        return suivi(etablissementId, anneeId);
    }

    public SuiviElevesDto.Suivi annulerAbandon(Long abandonId) {
        AbandonScolaire a = abandons.findById(abandonId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Abandon introuvable"));
        if (!perimetre.peutGererEtablissement(a.getEtablissementId())) {
            throw new AccessDeniedException("Abandon hors de votre établissement");
        }
        abandons.delete(a);
        audit.enregistrer("ABANDON_SCOLAIRE_ANNULE", "Eleve", a.getEleveId(), Map.of("etablissement", a.getEtablissementId()));
        return suivi(a.getEtablissementId(), a.getAnneeScolaireId());
    }

    public void enregistrerVulnerabilite(Long etablissementId, Long eleveId, SuiviElevesDto.Vulnerabilite q, String auteur) {
        long inscrit = ((Number) em.createNativeQuery("SELECT count(*) FROM inscription WHERE eleve_id = :el AND etablissement_id = :e AND statut = 'ACCEPTEE'")
                .setParameter("el", eleveId).setParameter("e", etablissementId).getSingleResult()).longValue();
        if (inscrit == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cet élève n'est pas inscrit dans cet établissement");
        }
        ProfilVulnerabiliteEleve p = profils.findById(eleveId).orElseGet(() -> {
            ProfilVulnerabiliteEleve n = new ProfilVulnerabiliteEleve();
            n.setEleveId(eleveId);
            return n;
        });
        p.setOrphelin(q.orphelin());
        p.setHandicap(q.handicap());
        p.setMisAJourLe(Instant.now(clock));
        p.setMisAJourPar(auteur);
        profils.save(p);
        audit.enregistrer("VULNERABILITE_ELEVE_MISE_A_JOUR", "Eleve", eleveId, Map.of("etablissement", etablissementId));
    }

    /** Totaux par établissement du périmètre de l'utilisateur : aucune donnée nominative. */
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public SuiviElevesDto.Synthese synthese(Long anneeId) {
        if (!annees.existsById(anneeId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Année scolaire introuvable");
        }
        UtilisateurPrincipal u = perimetre.utilisateurCourant();
        if (u == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }
        List<Etablissement> perimetreEtab = perimetre.etablissementsAccessibles(u);
        List<Long> ids = perimetreEtab.stream().map(Etablissement::getId).toList();
        if (ids.isEmpty()) {
            return new SuiviElevesDto.Synthese(anneeId, new SuiviElevesDto.Resume(0, 0, 0, null, 0, 0), List.of(), List.of());
        }
        Map<Long, int[]> eff = new HashMap<>();
        for (Object[] r : (List<Object[]>) em.createNativeQuery("SELECT i.etablissement_id, count(*) FROM inscription i WHERE i.annee_scolaire_id = :a "
                + "AND i.statut = 'ACCEPTEE' AND i.etablissement_id IN (:ids) GROUP BY i.etablissement_id").setParameter("a", anneeId).setParameter("ids", ids).getResultList()) {
            eff.put(((Number) r[0]).longValue(), new int[] {entier(r[1]), 0, 0, 0, 0});
        }
        for (Object[] r : (List<Object[]>) em.createNativeQuery("SELECT a.etablissement_id, count(*), count(*) FILTER (WHERE e.sexe = 'F') FROM abandon_scolaire a "
                + "JOIN eleve e ON e.id = a.eleve_id WHERE a.annee_scolaire_id = :a AND a.etablissement_id IN (:ids) GROUP BY a.etablissement_id")
                .setParameter("a", anneeId).setParameter("ids", ids).getResultList()) {
            int[] t = eff.computeIfAbsent(((Number) r[0]).longValue(), k -> new int[5]);
            t[1] = entier(r[1]);
            t[2] = entier(r[2]);
        }
        for (Object[] r : (List<Object[]>) em.createNativeQuery("SELECT i.etablissement_id, count(*) FILTER (WHERE p.orphelin <> 'AUCUN'), "
                + "count(*) FILTER (WHERE p.handicap IS NOT NULL) FROM inscription i JOIN profil_vulnerabilite_eleve p ON p.eleve_id = i.eleve_id "
                + "WHERE i.annee_scolaire_id = :a AND i.statut = 'ACCEPTEE' AND i.etablissement_id IN (:ids) GROUP BY i.etablissement_id")
                .setParameter("a", anneeId).setParameter("ids", ids).getResultList()) {
            int[] t = eff.computeIfAbsent(((Number) r[0]).longValue(), k -> new int[5]);
            t[3] = entier(r[1]);
            t[4] = entier(r[2]);
        }
        List<SuiviElevesDto.Motif> motifs = new ArrayList<>();
        for (Object[] r : (List<Object[]>) em.createNativeQuery("SELECT motif, count(*) FROM abandon_scolaire WHERE annee_scolaire_id = :a "
                + "AND etablissement_id IN (:ids) GROUP BY motif ORDER BY 2 DESC, 1").setParameter("a", anneeId).setParameter("ids", ids).getResultList()) {
            motifs.add(new SuiviElevesDto.Motif((String) r[0], entier(r[1])));
        }
        List<SuiviElevesDto.LigneEtablissement> lignes = new ArrayList<>();
        int[] total = new int[5];
        for (Etablissement e : perimetreEtab) {
            int[] t = eff.getOrDefault(e.getId(), new int[5]);
            for (int k = 0; k < 5; k++) {
                total[k] += t[k];
            }
            lignes.add(new SuiviElevesDto.LigneEtablissement(e.getId(), e.getNom(), e.getZone() == null ? null : e.getZone().getNom(), t[0], t[1], t[2],
                    taux(t[1], t[0]), t[3], t[4]));
        }
        lignes.sort(Comparator.comparing((SuiviElevesDto.LigneEtablissement l) -> l.tauxAbandon() == null ? -1.0 : l.tauxAbandon()).reversed()
                .thenComparing(SuiviElevesDto.LigneEtablissement::etablissement));
        return new SuiviElevesDto.Synthese(anneeId, new SuiviElevesDto.Resume(total[0], total[1], total[2], taux(total[1], total[0]), total[3], total[4]), motifs, lignes);
    }
}
