package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
}
