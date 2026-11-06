package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.DotationManuel;
import com.plateforme.scolaire.domain.Etablissement;
import com.plateforme.scolaire.domain.ManuelScolaire;
import com.plateforme.scolaire.repository.AnneeScolaireRepository;
import com.plateforme.scolaire.repository.DotationManuelRepository;
import com.plateforme.scolaire.repository.EtablissementRepository;
import com.plateforme.scolaire.repository.ManuelScolaireRepository;
import com.plateforme.scolaire.repository.MatiereRepository;
import com.plateforme.scolaire.repository.NiveauRepository;
import com.plateforme.scolaire.security.PerimetreService;
import com.plateforme.scolaire.security.UtilisateurPrincipal;
import com.plateforme.scolaire.web.dto.ManuelsDto;
import jakarta.persistence.EntityManager;
import java.time.Clock;
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
 * Manuels scolaires : le ministère tient le catalogue, chaque établissement déclare les exemplaires reçus, distribués et perdus.
 * L'objectif visé est un manuel par élève et par matière ; les seuils de l'état (100 % complet, 50 % partiel) sont des hypothèses à confirmer.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ManuelsService {

    private final ManuelScolaireRepository manuels;
    private final DotationManuelRepository dotations;
    private final MatiereRepository matieres;
    private final NiveauRepository niveaux;
    private final EtablissementRepository etablissements;
    private final AnneeScolaireRepository annees;
    private final PerimetreService perimetre;
    private final EntityManager em;
    private final AuditService audit;
    private final Clock clock;

    /** Pur : exemplaires utilisables = reçus moins perdus. */
    static int exemplaires(int recus, int perdus) {
        return Math.max(0, recus - perdus);
    }

    /** Pur : exemplaires en stock, non encore distribués. */
    static int disponibles(int recus, int distribues, int perdus) {
        return Math.max(0, recus - distribues - perdus);
    }

    /** Pur : exemplaires pour 100 élèves, une décimale ; null sans élève. */
    static Double couverture(int exemplaires, int effectif) {
        return effectif <= 0 ? null : Math.round(exemplaires * 1000.0 / effectif) / 10.0;
    }

    /** Pur : COMPLET, PARTIEL, INSUFFISANT ou SANS_ELEVE. */
    static String etat(Double couverture) {
        if (couverture == null) {
            return "SANS_ELEVE";
        }
        return couverture >= 100 ? "COMPLET" : couverture >= 50 ? "PARTIEL" : "INSUFFISANT";
    }

    /** Pur : message d'erreur de quantités incohérentes, sinon null. */
    static String verifier(int recus, int distribues, int perdus) {
        if (distribues + perdus > recus) {
            return "Les exemplaires distribués et perdus (" + (distribues + perdus) + ") dépassent les exemplaires reçus (" + recus + ")";
        }
        return null;
    }

    private static ResponseStatusException erreur(HttpStatus s, String m) {
        return new ResponseStatusException(s, m);
    }

    private ManuelsDto.Manuel dto(ManuelScolaire m) {
        return new ManuelsDto.Manuel(m.getId(), m.getMatiere().getId(), m.getMatiere().getLibelle(), m.getNiveau().getId(), m.getNiveau().getLibelle(), m.getTitre(),
                m.getEditeur(), m.getAnneeEdition(), m.getSerie());
    }

    @Transactional(readOnly = true)
    public List<ManuelsDto.Manuel> catalogue() {
        return manuels.findAllByOrderByNiveauOrdreAscMatiereLibelleAscTitreAsc().stream().map(this::dto).toList();
    }

    public ManuelsDto.Manuel ajouter(ManuelsDto.NouveauManuel q) {
        var matiere = matieres.findById(q.matiereId()).orElseThrow(() -> erreur(HttpStatus.BAD_REQUEST, "Matière inconnue"));
        var niveau = niveaux.findById(q.niveauId()).orElseThrow(() -> erreur(HttpStatus.BAD_REQUEST, "Niveau inconnu"));
        String serie;
        try {
            serie = com.plateforme.scolaire.domain.SerieLycee.normaliser(q.serie());
        } catch (IllegalArgumentException ex) {
            throw erreur(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
        if (manuels.existe(q.matiereId(), q.niveauId(), serie, q.titre().trim())) {
            throw erreur(HttpStatus.CONFLICT, "Ce manuel existe déjà pour cette matière, ce niveau et cette série");
        }
        ManuelScolaire m = new ManuelScolaire();
        m.setMatiere(matiere);
        m.setNiveau(niveau);
        m.setSerie(serie);
        m.setTitre(q.titre().trim());
        m.setEditeur(q.editeur() == null || q.editeur().isBlank() ? null : q.editeur().trim());
        m.setAnneeEdition(q.anneeEdition());
        manuels.save(m);
        audit.enregistrer("MANUEL_SCOLAIRE_AJOUTE", "ManuelScolaire", m.getId(), Map.of("titre", m.getTitre()));
        return dto(m);
    }

    public void supprimer(Long id) {
        ManuelScolaire m = manuels.findById(id).orElseThrow(() -> erreur(HttpStatus.NOT_FOUND, "Manuel introuvable"));
        if (dotations.existsByManuelId(id)) {
            throw erreur(HttpStatus.CONFLICT, "Ce manuel a déjà été déclaré par des établissements : il ne peut plus être supprimé");
        }
        manuels.delete(m);
        audit.enregistrer("MANUEL_SCOLAIRE_SUPPRIME", "ManuelScolaire", id, Map.of("titre", m.getTitre()));
    }

    /** Élèves acceptés par établissement et par niveau : clé « etablissement-niveau ». */
    @SuppressWarnings("unchecked")
    private Map<String, Integer> effectifs(Long anneeId, Long etablissementId) {
        String sql = "SELECT i.etablissement_id, c.niveau_id, c.serie, count(*) FROM inscription i JOIN classe c ON c.id = i.classe_id "
                + "WHERE i.annee_scolaire_id = :a AND i.statut = 'ACCEPTEE' " + (etablissementId == null ? "" : "AND i.etablissement_id = :e ")
                + "GROUP BY i.etablissement_id, c.niveau_id, c.serie";
        var requete = em.createNativeQuery(sql).setParameter("a", anneeId);
        if (etablissementId != null) {
            requete.setParameter("e", etablissementId);
        }
        Map<String, Integer> res = new HashMap<>();
        for (Object[] r : (List<Object[]>) requete.getResultList()) {
            String niveauCle = ((Number) r[0]).longValue() + "-" + ((Number) r[1]).longValue();
            int n = ((Number) r[3]).intValue();
            res.merge(niveauCle, n, Integer::sum);
            if (r[2] != null) {
                res.merge(niveauCle + "-" + r[2], n, Integer::sum);
            }
        }
        return res;
    }

    /**
     * Matières du programme officiel applicables à un type d'établissement, par niveau : clé « niveau-matière ».
     * Les niveaux sans aucune ligne de programme sont absents de la table (programme non renseigné : on ne filtre pas).
     */
    @SuppressWarnings("unchecked")
    private Map<Long, java.util.Set<Long>> matieresDuProgramme(Long anneeId, Long typeEtablissementId) {
        String sql = "SELECT niveau_id, matiere_id FROM programme_matiere WHERE annee_scolaire_id = :a AND (type_etablissement_id IS NULL"
                + (typeEtablissementId == null ? "" : " OR type_etablissement_id = :t") + ")";
        var requete = em.createNativeQuery(sql).setParameter("a", anneeId);
        if (typeEtablissementId != null) {
            requete.setParameter("t", typeEtablissementId);
        }
        Map<Long, java.util.Set<Long>> res = new HashMap<>();
        for (Object[] r : (List<Object[]>) requete.getResultList()) {
            res.computeIfAbsent(((Number) r[0]).longValue(), k -> new java.util.HashSet<>()).add(((Number) r[1]).longValue());
        }
        return res;
    }

    /** Pur : le manuel est-il au programme ? Vrai si le programme du niveau n'est pas renseigné. */
    static boolean auProgramme(Map<Long, java.util.Set<Long>> programme, Long niveauId, Long matiereId) {
        java.util.Set<Long> matieresDuNiveau = programme.get(niveauId);
        return matieresDuNiveau == null || matieresDuNiveau.contains(matiereId);
    }

    /** Pur : élèves concernés par un manuel : ceux du niveau, ou ceux de sa série s'il est propre à une série. */
    static int effectifDuManuel(Map<String, Integer> eff, Long etablissementId, Long niveauId, String serie) {
        return eff.getOrDefault(etablissementId + "-" + niveauId + (serie == null ? "" : "-" + serie), 0);
    }

    private ManuelsDto.Ligne ligne(ManuelScolaire m, int effectif, DotationManuel d) {
        int recus = d == null ? 0 : d.getRecus();
        int distribues = d == null ? 0 : d.getDistribues();
        int perdus = d == null ? 0 : d.getPerdus();
        int ex = exemplaires(recus, perdus);
        Double c = couverture(ex, effectif);
        return new ManuelsDto.Ligne(m.getId(), m.getMatiere().getLibelle(), m.getNiveau().getLibelle() + (m.getSerie() == null ? "" : " · " + m.getSerie().replace('_', ' ').toLowerCase()), m.getTitre(), effectif, recus, distribues, perdus,
                disponibles(recus, distribues, perdus), ex, c, etat(c), d == null ? null : d.getObservation());
    }

    @Transactional(readOnly = true)
    public ManuelsDto.Dotation dotation(Long etablissementId, Long anneeId) {
        Etablissement etab = etablissements.findById(etablissementId).orElseThrow(() -> erreur(HttpStatus.NOT_FOUND, "Établissement introuvable"));
        if (!annees.existsById(anneeId)) {
            throw erreur(HttpStatus.NOT_FOUND, "Année scolaire introuvable");
        }
        Map<Long, java.util.Set<Long>> programme = matieresDuProgramme(anneeId, etab.getTypeEtablissement() == null ? null : etab.getTypeEtablissement().getId());
        Map<String, Integer> eff = effectifs(anneeId, etablissementId);
        Map<Long, DotationManuel> parManuel = new HashMap<>();
        dotations.findByEtablissementIdAndAnneeScolaireId(etablissementId, anneeId).forEach(d -> parManuel.put(d.getManuelId(), d));
        List<ManuelsDto.Ligne> lignes = new ArrayList<>();
        for (ManuelScolaire m : manuels.findAllByOrderByNiveauOrdreAscMatiereLibelleAscTitreAsc()) {
            int effectif = effectifDuManuel(eff, etablissementId, m.getNiveau().getId(), m.getSerie());
            // On ne propose que les manuels des niveaux enseignés dans l'établissement dont la matière figure au programme du type d'établissement (ou déjà déclarés)
            boolean propose = effectif > 0 && auProgramme(programme, m.getNiveau().getId(), m.getMatiere().getId());
            if (propose || parManuel.containsKey(m.getId())) {
                lignes.add(ligne(m, effectif, parManuel.get(m.getId())));
            }
        }
        return new ManuelsDto.Dotation(etablissementId, anneeId, lignes);
    }

    public ManuelsDto.Dotation declarer(Long etablissementId, Long anneeId, Long manuelId, ManuelsDto.Quantites q, String auteur) {
        if (!etablissements.existsById(etablissementId) || !annees.existsById(anneeId)) {
            throw erreur(HttpStatus.NOT_FOUND, "Établissement ou année introuvable");
        }
        if (!manuels.existsById(manuelId)) {
            throw erreur(HttpStatus.NOT_FOUND, "Manuel introuvable");
        }
        String message = verifier(q.recus(), q.distribues(), q.perdus());
        if (message != null) {
            throw erreur(HttpStatus.BAD_REQUEST, message);
        }
        DotationManuel d = dotations.findByEtablissementIdAndAnneeScolaireIdAndManuelId(etablissementId, anneeId, manuelId).orElseGet(() -> {
            DotationManuel n = new DotationManuel();
            n.setEtablissementId(etablissementId);
            n.setAnneeScolaireId(anneeId);
            n.setManuelId(manuelId);
            return n;
        });
        d.setRecus(q.recus());
        d.setDistribues(q.distribues());
        d.setPerdus(q.perdus());
        d.setObservation(q.observation() == null || q.observation().isBlank() ? null : q.observation().trim());
        d.setMisAJourLe(Instant.now(clock));
        d.setMisAJourPar(auteur);
        dotations.save(d);
        audit.enregistrer("DOTATION_MANUEL_DECLAREE", "Etablissement", etablissementId, Map.of("manuel", manuelId, "annee", anneeId, "recus", q.recus()));
        return dotation(etablissementId, anneeId);
    }

    @Transactional(readOnly = true)
    public ManuelsDto.Synthese synthese(Long anneeId) {
        if (!annees.existsById(anneeId)) {
            throw erreur(HttpStatus.NOT_FOUND, "Année scolaire introuvable");
        }
        UtilisateurPrincipal p = perimetre.utilisateurCourant();
        if (p == null) {
            throw erreur(HttpStatus.FORBIDDEN, "Accès refusé");
        }
        Map<String, Integer> eff = effectifs(anneeId, null);
        Map<String, DotationManuel> dot = new HashMap<>();
        dotations.findByAnneeScolaireId(anneeId).forEach(d -> dot.put(d.getEtablissementId() + "-" + d.getManuelId(), d));
        List<ManuelScolaire> catalogue = manuels.findAllByOrderByNiveauOrdreAscMatiereLibelleAscTitreAsc();
        List<ManuelsDto.LigneEtablissement> lignes = new ArrayList<>();
        int besoinTotal = 0;
        int exTotal = 0;
        int insuffisants = 0;
        for (Etablissement e : perimetre.etablissementsAccessibles(p)) {
            int besoin = 0;
            int ex = 0;
            int concernes = 0;
            int manques = 0;
            for (ManuelScolaire m : catalogue) {
                int effectif = effectifDuManuel(eff, e.getId(), m.getNiveau().getId(), m.getSerie());
                if (effectif == 0) {
                    continue;
                }
                DotationManuel d = dot.get(e.getId() + "-" + m.getId());
                int exemplaires = d == null ? 0 : exemplaires(d.getRecus(), d.getPerdus());
                besoin += effectif;
                // On ne compte pas plus d'exemplaires que d'élèves : le surplus d'un manuel ne comble pas le manque d'un autre
                ex += Math.min(exemplaires, effectif);
                concernes++;
                if ("INSUFFISANT".equals(etat(couverture(exemplaires, effectif)))) {
                    manques++;
                }
            }
            if (concernes == 0) {
                continue;
            }
            besoinTotal += besoin;
            exTotal += ex;
            if (manques > 0) {
                insuffisants++;
            }
            lignes.add(new ManuelsDto.LigneEtablissement(e.getId(), e.getNom(), e.getZone() == null ? null : e.getZone().getNom(), besoin, ex, couverture(ex, besoin),
                    manques, concernes));
        }
        lignes.sort(Comparator.comparing((ManuelsDto.LigneEtablissement l) -> l.couverture() == null ? 101.0 : l.couverture()).thenComparing(ManuelsDto.LigneEtablissement::etablissement));
        return new ManuelsDto.Synthese(anneeId, besoinTotal, exTotal, couverture(exTotal, besoinTotal), insuffisants, lignes);
    }
}
