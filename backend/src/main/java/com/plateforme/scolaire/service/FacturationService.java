package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.AnneeScolaire;
import com.plateforme.scolaire.domain.Classe;
import com.plateforme.scolaire.domain.Etablissement;
import com.plateforme.scolaire.domain.Exoneration;
import com.plateforme.scolaire.domain.Facture;
import com.plateforme.scolaire.domain.Frais;
import com.plateforme.scolaire.domain.Inscription;
import com.plateforme.scolaire.domain.Niveau;
import com.plateforme.scolaire.domain.Utilisateur;
import com.plateforme.scolaire.repository.AnneeScolaireRepository;
import com.plateforme.scolaire.repository.ClasseRepository;
import com.plateforme.scolaire.repository.EtablissementRepository;
import com.plateforme.scolaire.repository.ExonerationRepository;
import com.plateforme.scolaire.repository.FactureRepository;
import com.plateforme.scolaire.repository.FraisRepository;
import com.plateforme.scolaire.repository.InscriptionRepository;
import com.plateforme.scolaire.repository.NiveauRepository;
import com.plateforme.scolaire.repository.PaiementRepository;
import com.plateforme.scolaire.repository.UtilisateurRepository;
import com.plateforme.scolaire.security.PerimetreService;
import com.plateforme.scolaire.security.UtilisateurPrincipal;
import com.plateforme.scolaire.web.dto.BilanFinancierDto;
import com.plateforme.scolaire.web.dto.ExonerationRequest;
import com.plateforme.scolaire.web.dto.FactureDto;
import com.plateforme.scolaire.web.dto.GenererFacturesRequest;
import com.plateforme.scolaire.web.dto.ResultatGenerationDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Émission des factures, exonérations et synthèse financière (Étape 3a).
 *
 * <p>Deux règles structurantes :</p>
 * <ul>
 *   <li>Le montant dû est <b>figé à l'émission</b> : modifier la grille tarifaire ensuite
 *       ne réécrit pas les factures déjà émises.</li>
 *   <li>La génération est <b>idempotente</b> : relancer produit les factures manquantes
 *       et ignore celles qui existent déjà (jamais de doublon).</li>
 * </ul>
 *
 * <p>Aucune donnée bancaire n'est manipulée ici. Les paiements arrivent en Étape 3b.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional
public class FacturationService {

    private static final String STATUT_IMPAYEE = "IMPAYEE";
    private static final String STATUT_PAYEE = "PAYEE";
    private static final String STATUT_EXONEREE = "EXONEREE";
    private static final String STATUT_ANNULEE = "ANNULEE";
    private static final Set<String> TYPES_EXONERATION = Set.of("BOURSE", "REMISE", "AIDE_SOCIALE", "CORRECTION");

    private final FactureRepository factureRepository;
    private final FraisRepository fraisRepository;
    private final ExonerationRepository exonerationRepository;
    private final EtablissementRepository etablissementRepository;
    private final AnneeScolaireRepository anneeRepository;
    private final ClasseRepository classeRepository;
    private final NiveauRepository niveauRepository;
    private final InscriptionRepository inscriptionRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final PerimetreService perimetreService;
    private final AuditService auditService;
    private final PaiementRepository paiementRepository;

    @PersistenceContext
    private EntityManager entityManager;

    // ---------- Consultation ----------

    @Transactional(readOnly = true)
    public List<FactureDto> facturesDeLEleve(Long eleveId) {
        return avecMontantsPayes(factureRepository.findByEleveIdOrderByDateEmissionDescIdDesc(eleveId)).stream()
                .map(FactureDto::de).toList();
    }

    @Transactional(readOnly = true)
    public List<FactureDto> facturesDeLEtablissement(Long etablissementId, Long anneeScolaireId, String statut) {
        if (!etablissementRepository.existsById(etablissementId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Établissement introuvable");
        }
        List<Facture> factures = statut == null || statut.isBlank()
                ? factureRepository.findByEtablissementIdAndAnneeScolaireIdOrderByDateEmissionDescIdDesc(
                        etablissementId, anneeScolaireId)
                : factureRepository.findByEtablissementIdAndAnneeScolaireIdAndStatutOrderByEleveNomAsc(
                        etablissementId, anneeScolaireId, statut);
        return avecMontantsPayes(factures).stream().map(FactureDto::de).toList();
    }

    @Transactional(readOnly = true)
    public FactureDto trouver(Long id) {
        return FactureDto.de(avecMontantPaye(charger(id)));
    }

    @Transactional(readOnly = true)
    public List<FactureDto> facturesEnRetard(Long etablissementId) {
        return avecMontantsPayes(factureRepository.findEnRetard(etablissementId, LocalDate.now())).stream()
                .map(FactureDto::de).toList();
    }

    /** Synthèse financière : total dû, exonéré, encaissé, restant, et comptage par statut. */
    @Transactional(readOnly = true)
    public BilanFinancierDto bilan(Long etablissementId, Long anneeScolaireId) {
        if (!etablissementRepository.existsById(etablissementId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Établissement introuvable");
        }
        List<Facture> factures = factureRepository
                .findByEtablissementIdAndAnneeScolaireIdOrderByDateEmissionDescIdDesc(etablissementId, anneeScolaireId);
        avecMontantsPayes(factures);

        BigDecimal totalDu = BigDecimal.ZERO;
        BigDecimal totalExonere = BigDecimal.ZERO;
        BigDecimal totalPaye = BigDecimal.ZERO;
        long impayees = 0;
        long payees = 0;
        long enRetard = 0;
        long nombreFactures = 0;
        String devise = null;

        for (Facture f : factures) {
            if (STATUT_ANNULEE.equals(f.getStatut())) {
                continue;
            }
            nombreFactures++;
            totalDu = totalDu.add(f.getMontantDu());
            totalExonere = totalExonere.add(f.getMontantExonere());
            BigDecimal paye = f.getMontantPaye() == null ? BigDecimal.ZERO : f.getMontantPaye();
            totalPaye = totalPaye.add(paye);
            devise = f.getDevise();
            if (STATUT_PAYEE.equals(f.getStatut()) || STATUT_EXONEREE.equals(f.getStatut())) {
                payees++;
            } else {
                impayees++;
                if (f.getEcheance() != null && f.getEcheance().isBefore(LocalDate.now())) {
                    enRetard++;
                }
            }
        }
        if (devise == null) {
            // Aucune facture encore : on affiche la devise de la grille (une seule par année).
            devise = fraisRepository.findByEtablissementIdAndAnneeScolaireIdOrderByLibelle(etablissementId, anneeScolaireId)
                    .stream().map(Frais::getDevise).findFirst().orElse(null);
        }
        BigDecimal restant = totalDu.subtract(totalExonere).subtract(totalPaye);
        return new BilanFinancierDto(etablissementId, anneeScolaireId, devise,
                totalDu, totalExonere, totalPaye, restant,
                nombreFactures, impayees, payees, enRetard);
    }

    // ---------- Génération ----------

    /**
     * Émet les factures manquantes pour une classe ou un niveau.
     * Relancer l'opération ne crée jamais de doublon.
     */
    public ResultatGenerationDto generer(Long etablissementId, GenererFacturesRequest r) {
        if (r.classeId() == null && r.niveauId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Précisez une classe ou un niveau");
        }
        if (r.classeId() != null && r.niveauId() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Choisissez soit une classe, soit un niveau, pas les deux");
        }
        Etablissement etablissement = etablissementRepository.findById(etablissementId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Établissement introuvable"));
        AnneeScolaire annee = anneeRepository.findById(r.anneeScolaireId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Année scolaire inconnue"));

        List<Inscription> inscriptions = new ArrayList<>();
        Long niveauCible;
        if (r.classeId() != null) {
            Classe classe = classeRepository.findById(r.classeId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Classe introuvable"));
            if (!classe.getEtablissement().getId().equals(etablissementId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cette classe n'appartient pas à cet établissement");
            }
            if (!classe.getAnneeScolaire().getId().equals(annee.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cette classe n'appartient pas à cette année scolaire");
            }
            inscriptions = inscriptionRepository
                    .findByClasseIdAndStatutOrderByEleveNomAscElevePrenomAsc(classe.getId(), "ACCEPTEE");
            niveauCible = classe.getNiveau().getId();
        } else {
            Niveau niveau = niveauRepository.findById(r.niveauId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Niveau inconnu"));
            // Seuls les élèves dont la classe est de CE niveau sont facturés.
            inscriptions = inscriptionRepository
                    .findByEtablissementIdAndAnneeScolaireIdOrderByDateDemandeDescIdDesc(etablissementId, annee.getId())
                    .stream()
                    .filter(i -> "ACCEPTEE".equals(i.getStatut()))
                    .filter(i -> i.getClasse() != null && niveau.getId().equals(i.getClasse().getNiveau().getId()))
                    .toList();
            niveauCible = niveau.getId();
        }

        if (inscriptions.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Aucun élève inscrit à facturer pour cette sélection");
        }

        List<Frais> grille = fraisRepository.findApplicables(etablissementId, annee.getId(), niveauCible)
                .stream().filter(Frais::isObligatoire).toList();
        if (grille.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Aucun frais obligatoire défini pour cette année et ce niveau");
        }

        int creees = 0;
        int ignorees = 0;
        Set<Long> elevesTraites = new HashSet<>();
        BigDecimal totalEmis = BigDecimal.ZERO;

        for (Inscription inscription : inscriptions) {
            Long eleveId = inscription.getEleve().getId();
            elevesTraites.add(eleveId);
            for (Frais frais : grille) {
                if (factureRepository.existsByFraisIdAndEleveId(frais.getId(), eleveId)) {
                    ignorees++;
                    continue;
                }
                Facture facture = new Facture();
                facture.setFrais(frais);
                facture.setEleve(inscription.getEleve());
                facture.setEtablissement(etablissement);
                facture.setAnneeScolaire(annee);
                facture.setLibelle(frais.getLibelle());
                facture.setMontantDu(frais.getMontant().setScale(2, RoundingMode.HALF_UP));
                facture.setDevise(frais.getDevise());
                facture.setEcheance(frais.getEcheance());
                facture.setStatut(STATUT_IMPAYEE);
                facture.setDateEmission(LocalDate.now());
                facture.setNumero(prochainNumero(etablissementId, annee.getId(), annee.getLibelle()));
                factureRepository.save(facture);
                creees++;
                totalEmis = totalEmis.add(facture.getMontantDu());
            }
        }

        auditService.enregistrer("GENERATION_FACTURES", "facture", null,
                Map.of("etablissementId", etablissementId, "anneeScolaireId", annee.getId(),
                        "creees", creees, "ignorees", ignorees,
                        "classeId", r.classeId() == null ? "aucune" : r.classeId(),
                        "niveauId", r.niveauId() == null ? "aucun" : r.niveauId()));

        return new ResultatGenerationDto(creees, ignorees, elevesTraites.size(), totalEmis);
    }

    // ---------- Exonérations ----------

    /** Applique une exonération sur une facture et ajuste son statut. */
    public FactureDto exonerer(Long factureId, ExonerationRequest r) {
        Facture facture = charger(factureId);
        if (STATUT_ANNULEE.equals(facture.getStatut())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette facture est annulée");
        }
        String type = r.typeExoneration() == null || r.typeExoneration().isBlank()
                ? "REMISE" : r.typeExoneration().trim().toUpperCase();
        if (!TYPES_EXONERATION.contains(type)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Type d'exonération inconnu (attendu : " + String.join(", ", new java.util.TreeSet<>(TYPES_EXONERATION)) + ")");
        }
        BigDecimal dejaPaye = montantPaye(facture.getId());
        if (dejaPaye.signum() > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Un paiement a déjà été encaissé sur cette facture : l'exonération doit passer par une correction");
        }
        verifierAucunPaiementEnCours(facture.getId());
        BigDecimal nouveauExonere = facture.getMontantExonere().add(r.montant());
        if (nouveauExonere.compareTo(facture.getMontantDu()) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le total exonéré dépasse le montant de la facture");
        }

        UtilisateurPrincipal courant = perimetreService.utilisateurCourant();
        Utilisateur decideur = utilisateurRepository.getReferenceById(courant.getId());

        Exoneration exoneration = new Exoneration();
        exoneration.setFacture(facture);
        exoneration.setMontant(r.montant());
        exoneration.setMotif(r.motif().trim());
        exoneration.setTypeExoneration(type);
        exoneration.setDecidePar(decideur);
        exonerationRepository.save(exoneration);

        facture.setMontantExonere(nouveauExonere);
        facture.setStatut(nouveauExonere.compareTo(facture.getMontantDu()) == 0 ? STATUT_EXONEREE : STATUT_IMPAYEE);

        auditService.enregistrer("EXONERATION_FACTURE", "facture", facture.getId(),
                Map.of("montant", r.montant().toString(), "motif", exoneration.getMotif(),
                        "type", exoneration.getTypeExoneration()));
        return FactureDto.de(avecMontantPaye(facture));
    }

    /** Annule une facture émise par erreur (aucun paiement encaissé). */
    public FactureDto annuler(Long factureId) {
        Facture facture = charger(factureId);
        if (montantPaye(facture.getId()).signum() > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Impossible d'annuler : des paiements ont été encaissés sur cette facture");
        }
        verifierAucunPaiementEnCours(facture.getId());
        facture.setStatut(STATUT_ANNULEE);
        auditService.enregistrer("ANNULATION_FACTURE", "facture", facture.getId(),
                Map.of("numero", facture.getNumero(), "montantDu", facture.getMontantDu().toString()));
        return FactureDto.de(avecMontantPaye(facture));
    }

    /** Recalcule le statut d'une facture après un encaissement. Utilisé par l'Étape 3b. */
    void rafraichirStatut(Facture facture) {
        if (STATUT_ANNULEE.equals(facture.getStatut())) {
            return;
        }
        BigDecimal du = facture.getMontantDu().subtract(facture.getMontantExonere());
        BigDecimal paye = montantPaye(facture.getId());
        if (paye.signum() == 0 && facture.getMontantExonere().compareTo(facture.getMontantDu()) == 0) {
            facture.setStatut(STATUT_EXONEREE);
        } else if (paye.compareTo(du) >= 0) {
            facture.setStatut(STATUT_PAYEE);
        } else if (paye.signum() > 0) {
            facture.setStatut("PARTIELLE");
        } else {
            facture.setStatut(STATUT_IMPAYEE);
        }
    }

    // ---------- Interne ----------

    private void verifierAucunPaiementEnCours(Long factureId) {
        if (paiementRepository.existsByFactureIdAndStatutAndCreatedAtAfter(
                factureId, "EN_ATTENTE", java.time.Instant.now().minus(PaiementService.DELAI_EN_ATTENTE))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Un paiement en ligne est en cours sur cette facture : réessayez dans quelques minutes");
        }
    }

    private Facture charger(Long id) {
        return factureRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Facture introuvable"));
    }

    /**
     * Numéro lisible et séquentiel par établissement et par année : FAC-2026-000001.
     * Le compteur est lu sous verrou pour éviter deux numéros identiques en cas
     * de générations simultanées.
     */
    @SuppressWarnings("unchecked")
    private String prochainNumero(Long etablissementId, Long anneeScolaireId, String libelleAnnee) {
        List<Number> lignes = entityManager.createNativeQuery(
                        "select dernier_numero from compteur_facture "
                                + "where etablissement_id = ?1 and annee_scolaire_id = ?2 for update")
                .setParameter(1, etablissementId)
                .setParameter(2, anneeScolaireId)
                .getResultList();
        long suivant;
        if (lignes.isEmpty()) {
            entityManager.createNativeQuery(
                            "insert into compteur_facture (etablissement_id, annee_scolaire_id, dernier_numero) "
                                    + "values (?1, ?2, 1)")
                    .setParameter(1, etablissementId)
                    .setParameter(2, anneeScolaireId)
                    .executeUpdate();
            suivant = 1;
        } else {
            suivant = lignes.get(0).longValue() + 1;
            entityManager.createNativeQuery(
                            "update compteur_facture set dernier_numero = ?3 "
                                    + "where etablissement_id = ?1 and annee_scolaire_id = ?2")
                    .setParameter(1, etablissementId)
                    .setParameter(2, anneeScolaireId)
                    .setParameter(3, suivant)
                    .executeUpdate();
        }
        // L'année scolaire sert de préfixe lisible : 2026-2027 -> FAC-2026-000001
        String prefixe = libelleAnnee == null || libelleAnnee.length() < 4
                ? "FAC"
                : "FAC-" + libelleAnnee.substring(0, 4);
        return String.format("%s-%06d", prefixe, suivant);
    }

    /** Montant déjà encaissé sur une facture (paiements validés uniquement). */
    private BigDecimal montantPaye(Long factureId) {
        BigDecimal total = paiementRepository.totalValide(factureId);
        return total == null ? BigDecimal.ZERO : total;
    }

    /** Renseigne le montant payé de toutes les factures en une seule requête. */
    private List<Facture> avecMontantsPayes(List<Facture> factures) {
        if (factures.isEmpty()) {
            return factures;
        }
        Map<Long, BigDecimal> payes = new HashMap<>();
        for (Object[] ligne : paiementRepository.totauxValides(factures.stream().map(Facture::getId).toList())) {
            payes.put(((Number) ligne[0]).longValue(), (BigDecimal) ligne[1]);
        }
        factures.forEach(f -> f.setMontantPaye(payes.getOrDefault(f.getId(), BigDecimal.ZERO)));
        return factures;
    }

    private Facture avecMontantPaye(Facture facture) {
        facture.setMontantPaye(montantPaye(facture.getId()));
        return facture;
    }
}
