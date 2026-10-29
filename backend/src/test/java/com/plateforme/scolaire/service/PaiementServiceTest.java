package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.plateforme.scolaire.domain.Eleve;
import com.plateforme.scolaire.domain.Facture;
import com.plateforme.scolaire.domain.Paiement;
import com.plateforme.scolaire.repository.EleveRepository;
import com.plateforme.scolaire.repository.FactureRepository;
import com.plateforme.scolaire.repository.LienParentalRepository;
import com.plateforme.scolaire.repository.PaiementRepository;
import com.plateforme.scolaire.repository.UtilisateurRepository;
import com.plateforme.scolaire.security.PerimetreService;
import com.plateforme.scolaire.web.dto.NotificationPaiement;
import com.plateforme.scolaire.web.dto.PaiementDto;
import com.plateforme.scolaire.web.dto.PaiementInitieDto;
import com.plateforme.scolaire.web.dto.PaiementRequest;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

class PaiementServiceTest {

    private static final long FACTURE = 30L;
    private static final long ELEVE = 100L;
    private static final long UTILISATEUR = 7L;
    private static final String SECRET = "secret-de-test";

    private final PaiementRepository paiementRepository = mock(PaiementRepository.class);
    private final FactureRepository factureRepository = mock(FactureRepository.class);
    private final EleveRepository eleveRepository = mock(EleveRepository.class);
    private final LienParentalRepository lienParentalRepository = mock(LienParentalRepository.class);
    private final FacturationService facturationService = mock(FacturationService.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<PasserelleDePaiement> passerelle = mock(ObjectProvider.class);
    private final Clock horloge = Clock.fixed(Instant.parse("2026-10-28T10:00:00Z"), ZoneOffset.UTC);

    private final PaiementService service = new PaiementService(paiementRepository, factureRepository,
            eleveRepository, lienParentalRepository, mock(UtilisateurRepository.class), facturationService,
            mock(PerimetreService.class), mock(AuditService.class), passerelle, horloge);

    private Facture facture;

    @BeforeEach
    void preparer() {
        Eleve eleve = new Eleve();
        eleve.setId(ELEVE);
        facture = new Facture();
        facture.setId(FACTURE);
        facture.setNumero("FAC-2026-000001");
        facture.setEleve(eleve);
        facture.setMontantDu(new BigDecimal("100.00"));
        facture.setMontantExonere(new BigDecimal("20.00"));
        facture.setDevise("EUR");
        facture.setStatut("IMPAYEE");
        when(factureRepository.findByIdPourMiseAJour(FACTURE)).thenReturn(Optional.of(facture));
        ReflectionTestUtils.setField(service, "secretWebhook", SECRET);
    }

    // ---------- Hors ligne ----------

    @Test
    void unEncaissementHorsLigneEstValideEtRecalculeLeStatut() {
        PaiementDto dto = service.enregistrerHorsLigne(FACTURE,
                new PaiementRequest(new BigDecimal("50.00"), "especes", null));

        assertThat(dto.statut()).isEqualTo("VALIDE");
        assertThat(dto.mode()).isEqualTo("ESPECES");
        assertThat(dto.devise()).isEqualTo("EUR");
        verify(facturationService).rafraichirStatut(facture);
    }

    @Test
    void onNeDepassePasLeResteAPayerExonerationEtPaiementsDeduits() {
        when(paiementRepository.totalValide(FACTURE)).thenReturn(new BigDecimal("30.00"));

        // dû 100 - exonéré 20 - déjà payé 30 = 50 restants
        assertThatThrownBy(() -> service.enregistrerHorsLigne(FACTURE,
                new PaiementRequest(new BigDecimal("50.01"), "VIREMENT", "VIR-1")))
                .isInstanceOf(ResponseStatusException.class);
        verify(paiementRepository, never()).save(any(Paiement.class));
    }

    @Test
    void leMobileMoneyNePasseJamaisParLaSaisieManuelle() {
        assertThatThrownBy(() -> service.enregistrerHorsLigne(FACTURE,
                new PaiementRequest(new BigDecimal("10.00"), "MOBILE_MONEY", null)))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void uneFactureDejaPayeeOuAnnuleeNAcceptePlusDePaiement() {
        facture.setStatut("PAYEE");

        assertThatThrownBy(() -> service.enregistrerHorsLigne(FACTURE,
                new PaiementRequest(new BigDecimal("1.00"), "ESPECES", null)))
                .isInstanceOf(ResponseStatusException.class);
    }

    // ---------- En ligne ----------

    @Test
    void sansPrestataireLePaiementEnLigneEstIndisponible() {
        when(passerelle.getIfAvailable()).thenReturn(null);

        assertThatThrownBy(() -> service.initierEnLigne(UTILISATEUR, FACTURE, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("503");
    }

    @Test
    void ontNePeutPayerQueLesFacturesDeSaFamille() {
        when(passerelle.getIfAvailable()).thenReturn(new PasserelleSimulee("http://localhost"));
        when(eleveRepository.findByUtilisateurId(UTILISATEUR)).thenReturn(Optional.empty());
        when(lienParentalRepository.existsByEleveIdAndUtilisateurId(ELEVE, UTILISATEUR)).thenReturn(false);

        assertThatThrownBy(() -> service.initierEnLigne(UTILISATEUR, FACTURE, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
        verify(paiementRepository, never()).save(any(Paiement.class));
    }

    @Test
    void unParentInitieLePaiementDuSoldeEtRecoitLUrlDuPrestataire() {
        when(passerelle.getIfAvailable()).thenReturn(new PasserelleSimulee("http://localhost"));
        when(lienParentalRepository.existsByEleveIdAndUtilisateurId(ELEVE, UTILISATEUR)).thenReturn(true);

        PaiementInitieDto initie = service.initierEnLigne(UTILISATEUR, FACTURE, null);

        assertThat(initie.montant()).isEqualByComparingTo("80.00");
        assertThat(initie.urlPaiement()).startsWith("http://localhost/paiement-simule/PAY-");
    }

    @Test
    void unPaiementDejaOuvertEstRepris_sansEnCreerUnSecond() {
        when(passerelle.getIfAvailable()).thenReturn(new PasserelleSimulee("http://localhost"));
        when(lienParentalRepository.existsByEleveIdAndUtilisateurId(ELEVE, UTILISATEUR)).thenReturn(true);
        when(paiementRepository.totalEnAttente(eq(FACTURE), any())).thenReturn(new BigDecimal("80.00"));
        Paiement ouvert = enAttente();
        ouvert.setUrlPaiement("http://localhost/paiement-simule/PAY-ABC");
        when(paiementRepository.findFirstByFactureIdAndStatutAndCreatedAtAfterOrderByCreatedAtDesc(
                eq(FACTURE), eq("EN_ATTENTE"), any())).thenReturn(Optional.of(ouvert));

        PaiementInitieDto repris = service.initierEnLigne(UTILISATEUR, FACTURE, null);

        assertThat(repris.reference()).isEqualTo("PAY-ABC");
        assertThat(repris.urlPaiement()).isEqualTo("http://localhost/paiement-simule/PAY-ABC");
        verify(paiementRepository, never()).save(any());
    }

    @Test
    void unPaiementOuvertSansAdresseConnueDonneUnMessageClair() {
        when(passerelle.getIfAvailable()).thenReturn(new PasserelleSimulee("http://localhost"));
        when(lienParentalRepository.existsByEleveIdAndUtilisateurId(ELEVE, UTILISATEUR)).thenReturn(true);
        when(paiementRepository.totalEnAttente(eq(FACTURE), any())).thenReturn(new BigDecimal("80.00"));
        when(paiementRepository.findFirstByFactureIdAndStatutAndCreatedAtAfterOrderByCreatedAtDesc(
                eq(FACTURE), eq("EN_ATTENTE"), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.initierEnLigne(UTILISATEUR, FACTURE, null))
                .hasMessageContaining("déjà en cours")
                .hasMessageContaining("30 minutes");
    }

    // ---------- Annulation ----------

    private Paiement valide(String mode) {
        Paiement p = new Paiement();
        p.setId(9L);
        p.setFacture(facture);
        p.setMontant(new BigDecimal("30.00"));
        p.setMode(mode);
        p.setStatut("VALIDE");
        p.setReferenceInterne("PAY-VAL");
        when(paiementRepository.findById(9L)).thenReturn(Optional.of(p));
        return p;
    }

    @Test
    void annulerUnEncaissementHorsLigneLeConserveAvecSonMotifEtRecalculeLaFacture() {
        Paiement p = valide("ESPECES");

        var dto = service.annulerHorsLigne(FACTURE, 9L, "  Erreur de saisie ");

        assertThat(dto.statut()).isEqualTo("ANNULE");
        assertThat(dto.motifAnnulation()).isEqualTo("Erreur de saisie");
        assertThat(p.getAnnuleLe()).isEqualTo(Instant.parse("2026-10-28T10:00:00Z"));
        verify(facturationService).rafraichirStatut(facture);
    }

    @Test
    void unPaiementEnLigneNeSAnnulePasIci() {
        valide("MOBILE_MONEY");

        assertThatThrownBy(() -> service.annulerHorsLigne(FACTURE, 9L, "test"))
                .hasMessageContaining("prestataire");
    }

    @Test
    void seulUnPaiementValidePeutEtreAnnule() {
        Paiement p = valide("CHEQUE");
        p.setStatut("ANNULE");

        assertThatThrownBy(() -> service.annulerHorsLigne(FACTURE, 9L, "test"))
                .hasMessageContaining("validé");
    }

    @Test
    void unPaiementDUneAutreFactureEstIntrouvable() {
        Paiement p = valide("ESPECES");
        Facture autre = new Facture();
        autre.setId(99L);
        p.setFacture(autre);

        assertThatThrownBy(() -> service.annulerHorsLigne(FACTURE, 9L, "test"))
                .hasMessageContaining("Paiement introuvable");
    }

    // ---------- Notification signée ----------

    private Paiement enAttente() {
        Paiement p = new Paiement();
        p.setId(1L);
        p.setFacture(facture);
        p.setMontant(new BigDecimal("80.00"));
        p.setStatut("EN_ATTENTE");
        p.setReferenceInterne("PAY-ABC");
        p.setReferenceExterne("SIM-PAY-ABC");
        when(paiementRepository.findFactureIdByReferenceInterne("PAY-ABC")).thenReturn(Optional.of(FACTURE));
        when(paiementRepository.findByReferenceInterne("PAY-ABC")).thenReturn(Optional.of(p));
        return p;
    }

    private static NotificationPaiement notification(String statut, String montant, String signature) {
        return new NotificationPaiement("PAY-ABC", statut, new BigDecimal(montant), "SIM-PAY-ABC", signature);
    }

    private static String bonneSignature(String statut, String montant) {
        return PaiementService.signer(SECRET, "PAY-ABC", statut, new BigDecimal(montant), "SIM-PAY-ABC");
    }

    @Test
    void uneSignatureFausseEstRefuseeEtRienNEstValide() {
        Paiement p = enAttente();

        assertThatThrownBy(() -> service.traiterNotification(notification("SUCCES", "80.00", "0000")))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(p.getStatut()).isEqualTo("EN_ATTENTE");
    }

    @Test
    void uneNotificationValideValideLePaiementEtRecalculeLaFacture() {
        Paiement p = enAttente();

        service.traiterNotification(notification("SUCCES", "80.00", bonneSignature("SUCCES", "80.00")));

        assertThat(p.getStatut()).isEqualTo("VALIDE");
        assertThat(p.getValideLe()).isNotNull();
        verify(facturationService).rafraichirStatut(facture);
    }

    @Test
    void rejouerUneNotificationNeChangeRien() {
        Paiement p = enAttente();
        p.setStatut("VALIDE");

        service.traiterNotification(notification("SUCCES", "80.00", bonneSignature("SUCCES", "80.00")));

        verify(facturationService, never()).rafraichirStatut(any(Facture.class));
    }

    @Test
    void unMontantDifferentDeCeluiAttenduNEstJamaisValide() {
        Paiement p = enAttente();

        service.traiterNotification(notification("SUCCES", "1.00", bonneSignature("SUCCES", "1.00")));

        assertThat(p.getStatut()).isEqualTo("ECHEC");
        verify(facturationService, never()).rafraichirStatut(any(Facture.class));
    }

    @Test
    void unEchecDuPrestataireMarqueLePaiementEnEchec() {
        Paiement p = enAttente();

        service.traiterNotification(notification("ECHEC", "80.00", bonneSignature("ECHEC", "80.00")));

        assertThat(p.getStatut()).isEqualTo("ECHEC");
    }

    @Test
    void sansSecretConfigureLesNotificationsSontRefusees() {
        ReflectionTestUtils.setField(service, "secretWebhook", "");

        assertThatThrownBy(() -> service.traiterNotification(notification("SUCCES", "80.00", "abc")))
                .isInstanceOf(ResponseStatusException.class);
    }

    // ---------- Expiration ----------

    private Paiement enAttente(Instant creeLe) {
        Paiement p = new Paiement();
        p.setId(9L);
        p.setFacture(facture);
        p.setMontant(new BigDecimal("30.00"));
        p.setMode("MOBILE_MONEY");
        p.setStatut("EN_ATTENTE");
        p.setReferenceInterne("PAY-ATT");
        p.setCreatedAt(creeLe);
        when(paiementRepository.findById(9L)).thenReturn(Optional.of(p));
        return p;
    }

    @Test
    void unPaiementOuvertDepuisTropLongtempsEstCloture() {
        Paiement p = enAttente(Instant.parse("2026-10-26T10:00:00Z"));

        boolean cloture = service.expirer(9L, Instant.parse("2026-10-27T10:00:00Z"));

        assertThat(cloture).isTrue();
        assertThat(p.getStatut()).isEqualTo("ECHEC");
    }

    @Test
    void unPaiementDejaTraiteOuRecentNEstPasTouche() {
        Paiement valide = enAttente(Instant.parse("2026-10-26T10:00:00Z"));
        valide.setStatut("VALIDE");
        assertThat(service.expirer(9L, Instant.parse("2026-10-27T10:00:00Z"))).isFalse();
        assertThat(valide.getStatut()).isEqualTo("VALIDE");

        Paiement recent = enAttente(Instant.parse("2026-10-28T09:00:00Z"));
        assertThat(service.expirer(9L, Instant.parse("2026-10-27T10:00:00Z"))).isFalse();
        assertThat(recent.getStatut()).isEqualTo("EN_ATTENTE");
    }
}
