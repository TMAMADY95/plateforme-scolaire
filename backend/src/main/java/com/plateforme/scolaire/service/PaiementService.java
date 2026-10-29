package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.Facture;
import com.plateforme.scolaire.domain.Paiement;
import com.plateforme.scolaire.repository.EleveRepository;
import com.plateforme.scolaire.repository.FactureRepository;
import com.plateforme.scolaire.repository.LienParentalRepository;
import com.plateforme.scolaire.repository.PaiementRepository;
import com.plateforme.scolaire.repository.UtilisateurRepository;
import com.plateforme.scolaire.security.PerimetreService;
import com.plateforme.scolaire.security.UtilisateurPrincipal;
import com.plateforme.scolaire.web.dto.NotificationPaiement;
import com.plateforme.scolaire.web.dto.PaiementDto;
import com.plateforme.scolaire.web.dto.PaiementInitieDto;
import com.plateforme.scolaire.web.dto.PaiementRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Encaissements (Étape 3b).
 *
 * <ul>
 *   <li><b>Hors ligne</b> (espèces, virement, chèque) : saisi par le personnel de l'établissement.</li>
 *   <li><b>En ligne</b> : initié par l'élève ou le parent, confirmé par une notification SIGNÉE du prestataire.
 *       Rien n'est validé sur la seule parole du navigateur du payeur.</li>
 * </ul>
 *
 * <p>La facture est verrouillée pendant chaque opération : deux paiements simultanés ne peuvent pas
 * dépasser le montant dû. Aucune donnée bancaire n'est jamais reçue ni stockée.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PaiementService {

    /** Au-delà, un paiement en ligne resté ouvert ne réserve plus de montant. */
    public static final Duration DELAI_EN_ATTENTE = Duration.ofMinutes(30);

    private static final Set<String> MODES_HORS_LIGNE = Set.of("ESPECES", "VIREMENT", "CHEQUE");
    private static final String VALIDE = "VALIDE";
    private static final String EN_ATTENTE = "EN_ATTENTE";
    private static final String ECHEC = "ECHEC";
    private static final String ANNULE = "ANNULE";

    private final PaiementRepository paiementRepository;
    private final FactureRepository factureRepository;
    private final EleveRepository eleveRepository;
    private final LienParentalRepository lienParentalRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final FacturationService facturationService;
    private final PerimetreService perimetreService;
    private final AuditService auditService;
    private final ObjectProvider<PasserelleDePaiement> passerelle;
    private final Clock clock;

    @Value("${app.paiement.secret-webhook:}")
    private String secretWebhook;

    // ---------- Consultation ----------

    @Transactional(readOnly = true)
    public List<PaiementDto> lister(Long factureId) {
        if (!factureRepository.existsById(factureId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Facture introuvable");
        }
        return paiementRepository.findByFactureIdOrderByCreatedAtDescIdDesc(factureId).stream()
                .map(PaiementDto::de).toList();
    }

    // ---------- Hors ligne ----------

    public PaiementDto enregistrerHorsLigne(Long factureId, PaiementRequest r) {
        String mode = r.mode().trim().toUpperCase();
        if (!MODES_HORS_LIGNE.contains(mode)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Mode inconnu (attendu : CHEQUE, ESPECES ou VIREMENT)");
        }
        Facture facture = verrouiller(factureId);
        verifierPayable(facture);

        BigDecimal montant = r.montant().setScale(2, RoundingMode.HALF_UP);
        BigDecimal reste = resteAPayer(facture);
        if (montant.compareTo(reste) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le montant dépasse le reste à payer (" + reste.toPlainString() + " " + facture.getDevise() + ")");
        }

        Paiement paiement = nouveau(facture, montant, mode, VALIDE);
        paiement.setValideLe(Instant.now(clock));
        paiement.setReferenceExterne(r.reference() == null || r.reference().isBlank() ? null : r.reference().trim());
        UtilisateurPrincipal courant = perimetreService.utilisateurCourant();
        if (courant != null) {
            paiement.setEnregistrePar(utilisateurRepository.getReferenceById(courant.getId()));
        }
        paiementRepository.save(paiement);
        facturationService.rafraichirStatut(facture);

        auditService.enregistrer("ENCAISSEMENT", "paiement", paiement.getId(),
                Map.of("facture", facture.getNumero(), "montant", montant.toPlainString(), "mode", mode));
        return PaiementDto.de(paiement);
    }

    /**
     * Annule un paiement validé saisi hors ligne (erreur de saisie, chèque rejeté...). Rien n'est supprimé :
     * le paiement passe en ANNULE avec son motif, et la facture retrouve son solde. Un paiement en ligne se
     * rembourse auprès du prestataire : l'annuler ici créerait un écart avec son relevé.
     */
    public PaiementDto annulerHorsLigne(Long factureId, Long paiementId, String motif) {
        Facture facture = verrouiller(factureId);
        Paiement paiement = paiementRepository.findById(paiementId)
                .filter(p -> p.getFacture().getId().equals(factureId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Paiement introuvable"));
        if (!VALIDE.equals(paiement.getStatut())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Seul un paiement validé peut être annulé");
        }
        if (!MODES_HORS_LIGNE.contains(paiement.getMode())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Un paiement en ligne ne s'annule pas ici : il se rembourse auprès du prestataire de paiement");
        }

        paiement.setStatut(ANNULE);
        paiement.setAnnuleLe(Instant.now(clock));
        paiement.setMotifAnnulation(motif.trim());
        UtilisateurPrincipal courant = perimetreService.utilisateurCourant();
        if (courant != null) {
            paiement.setAnnulePar(utilisateurRepository.getReferenceById(courant.getId()));
        }
        paiementRepository.save(paiement);
        facturationService.rafraichirStatut(facture);

        auditService.enregistrer("ANNULATION_PAIEMENT", "paiement", paiement.getId(),
                Map.of("facture", facture.getNumero(), "montant", paiement.getMontant().toPlainString(),
                        "mode", paiement.getMode(), "motif", paiement.getMotifAnnulation()));
        return PaiementDto.de(paiement);
    }

    // ---------- En ligne ----------

    public PaiementInitieDto initierEnLigne(Long utilisateurId, Long factureId, BigDecimal montantDemande) {
        PasserelleDePaiement prestataire = passerelle.getIfAvailable();
        if (prestataire == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Le paiement en ligne n'est pas activé pour le moment");
        }
        Facture facture = verrouiller(factureId);
        if (!estProprietaire(utilisateurId, facture.getEleve().getId())) {
            // Même réponse qu'une facture inexistante : on ne révèle rien sur les autres familles.
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Facture introuvable");
        }
        verifierPayable(facture);

        Instant limite = Instant.now(clock).minus(DELAI_EN_ATTENTE);
        BigDecimal enAttente = nvl(paiementRepository.totalEnAttente(facture.getId(), limite));
        BigDecimal disponible = resteAPayer(facture).subtract(enAttente);
        if (disponible.signum() <= 0) {
            // Reprise : le solde est déjà couvert par un paiement ouvert, on renvoie le même lien plutôt que d'en créer un autre.
            if (montantDemande == null) {
                Optional<Paiement> ouvert = paiementRepository
                        .findFirstByFactureIdAndStatutAndCreatedAtAfterOrderByCreatedAtDesc(
                                facture.getId(), EN_ATTENTE, limite);
                if (ouvert.isPresent() && ouvert.get().getUrlPaiement() != null) {
                    Paiement repris = ouvert.get();
                    return new PaiementInitieDto(repris.getReferenceInterne(), repris.getUrlPaiement(),
                            repris.getMontant(), facture.getDevise());
                }
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Un paiement est déjà en cours pour cette facture. Terminez-le auprès du prestataire, "
                            + "ou réessayez dans " + DELAI_EN_ATTENTE.toMinutes() + " minutes au plus.");
        }
        BigDecimal montant = montantDemande == null ? disponible : montantDemande.setScale(2, RoundingMode.HALF_UP);
        if (montant.signum() <= 0 || montant.compareTo(disponible) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Montant invalide (maximum : " + disponible.toPlainString() + " " + facture.getDevise() + ")");
        }

        Paiement paiement = nouveau(facture, montant, "MOBILE_MONEY", EN_ATTENTE);
        PasserelleDePaiement.ResultatInitiation resultat = prestataire.initier(new PasserelleDePaiement.DemandePaiement(
                paiement.getReferenceInterne(), montant, facture.getDevise(), facture.getLibelle()));
        paiement.setReferenceExterne(resultat.referenceExterne());
        paiement.setUrlPaiement(resultat.urlPaiement());
        paiementRepository.save(paiement);

        auditService.enregistrer("INITIATION_PAIEMENT", "paiement", paiement.getId(),
                Map.of("facture", facture.getNumero(), "montant", montant.toPlainString()));
        return new PaiementInitieDto(paiement.getReferenceInterne(), resultat.urlPaiement(), montant, facture.getDevise());
    }

    /**
     * Notification du prestataire. Idempotente : la rejouer ne change rien. Vérifie la signature, la
     * cohérence du montant et de la référence avant de valider quoi que ce soit.
     */
    public void traiterNotification(NotificationPaiement n) {
        if (secretWebhook == null || secretWebhook.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Notifications de paiement non configurées");
        }
        String attendue = signer(secretWebhook, n.reference(), n.statut(), n.montant(), n.referenceExterne());
        if (!MessageDigest.isEqual(attendue.getBytes(StandardCharsets.UTF_8),
                n.signature().trim().toLowerCase().getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Signature invalide");
        }

        Long factureId = paiementRepository.findFactureIdByReferenceInterne(n.reference())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Paiement introuvable"));
        Facture facture = verrouiller(factureId);
        Paiement paiement = paiementRepository.findByReferenceInterne(n.reference())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Paiement introuvable"));

        if (!EN_ATTENTE.equals(paiement.getStatut())) {
            return; // déjà traité : notification rejouée
        }
        if (paiement.getReferenceExterne() != null && n.referenceExterne() != null
                && !paiement.getReferenceExterne().equals(n.referenceExterne())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Référence de transaction incohérente");
        }

        if (!"SUCCES".equals(n.statut())) {
            paiement.setStatut(ECHEC);
            auditService.enregistrer("PAIEMENT_ECHEC", "paiement", paiement.getId(),
                    Map.of("reference", paiement.getReferenceInterne()));
            return;
        }
        if (n.montant().setScale(2, RoundingMode.HALF_UP).compareTo(paiement.getMontant()) != 0) {
            paiement.setStatut(ECHEC);
            auditService.enregistrer("PAIEMENT_MONTANT_INCOHERENT", "paiement", paiement.getId(),
                    Map.of("attendu", paiement.getMontant().toPlainString(), "recu", n.montant().toPlainString()));
            return;
        }

        paiement.setStatut(VALIDE);
        paiement.setValideLe(Instant.now(clock));
        facturationService.rafraichirStatut(facture);
        BigDecimal reste = resteAPayer(facture);
        auditService.enregistrer(reste.signum() < 0 ? "PAIEMENT_SURPAIEMENT" : "PAIEMENT_VALIDE", "paiement",
                paiement.getId(), Map.of("reference", paiement.getReferenceInterne(),
                        "montant", paiement.getMontant().toPlainString()));
    }

    /**
     * Clôture un paiement en ligne resté EN_ATTENTE trop longtemps (statut ECHEC). Verrouille la facture avant de
     * relire le paiement, comme le fait la notification du prestataire : si celle-ci est arrivée entre-temps, le
     * paiement n'est plus EN_ATTENTE et rien n'est modifié.
     *
     * @return true si le paiement a été clôturé
     */
    public boolean expirer(Long paiementId, Instant avant) {
        Long factureId = paiementRepository.findById(paiementId)
                .map(p -> p.getFacture().getId()).orElse(null);
        if (factureId == null) {
            return false;
        }
        verrouiller(factureId);
        Paiement paiement = paiementRepository.findById(paiementId).orElse(null);
        if (paiement == null || !EN_ATTENTE.equals(paiement.getStatut()) || !paiement.getCreatedAt().isBefore(avant)) {
            return false;
        }
        paiement.setStatut(ECHEC);
        auditService.enregistrer("PAIEMENT_EXPIRE", "paiement", paiement.getId(),
                Map.of("reference", paiement.getReferenceInterne()));
        return true;
    }

    /** Signature HMAC-SHA256 (hexadécimal minuscule) d'une notification. */
    public static String signer(String secret, String reference, String statut, BigDecimal montant,
                                String referenceExterne) {
        String message = reference + "|" + statut + "|" + montant.setScale(2, RoundingMode.HALF_UP).toPlainString()
                + "|" + (referenceExterne == null ? "" : referenceExterne);
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA256 indisponible", e);
        }
    }

    // ---------- Interne ----------

    private Facture verrouiller(Long factureId) {
        return factureRepository.findByIdPourMiseAJour(factureId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Facture introuvable"));
    }

    private static void verifierPayable(Facture facture) {
        if (Set.of("ANNULEE", "EXONEREE", "PAYEE").contains(facture.getStatut())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cette facture n'attend plus de paiement (statut " + facture.getStatut() + ")");
        }
    }

    /** Montant dû, exonération déduite, moins ce qui est déjà encaissé. */
    private BigDecimal resteAPayer(Facture facture) {
        return facture.getMontantDu().subtract(facture.getMontantExonere())
                .subtract(nvl(paiementRepository.totalValide(facture.getId())));
    }

    private boolean estProprietaire(Long utilisateurId, Long eleveId) {
        boolean sonDossier = eleveRepository.findByUtilisateurId(utilisateurId)
                .map(e -> e.getId().equals(eleveId)).orElse(false);
        return sonDossier || lienParentalRepository.existsByEleveIdAndUtilisateurId(eleveId, utilisateurId);
    }

    private Paiement nouveau(Facture facture, BigDecimal montant, String mode, String statut) {
        Paiement p = new Paiement();
        p.setFacture(facture);
        p.setMontant(montant);
        p.setDevise(facture.getDevise());
        p.setMode(mode);
        p.setStatut(statut);
        p.setReferenceInterne("PAY-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase());
        p.setCreatedAt(Instant.now(clock));
        return p;
    }

    private static BigDecimal nvl(BigDecimal valeur) {
        return valeur == null ? BigDecimal.ZERO : valeur;
    }
}
