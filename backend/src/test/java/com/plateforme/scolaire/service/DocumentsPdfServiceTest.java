package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.plateforme.scolaire.domain.Eleve;
import com.plateforme.scolaire.domain.Etablissement;
import com.plateforme.scolaire.domain.Facture;
import com.plateforme.scolaire.domain.Paiement;
import com.plateforme.scolaire.repository.ClasseRepository;
import com.plateforme.scolaire.repository.EleveRepository;
import com.plateforme.scolaire.repository.FactureRepository;
import com.plateforme.scolaire.repository.LienParentalRepository;
import com.plateforme.scolaire.repository.PaiementRepository;
import com.plateforme.scolaire.security.PerimetreFinancesService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class DocumentsPdfServiceTest {

    private static final Long FACTURE = 5L;
    private static final Long ELEVE = 7L;
    private static final Long PARENT = 20L;

    private final FactureRepository factureRepository = mock(FactureRepository.class);
    private final PaiementRepository paiementRepository = mock(PaiementRepository.class);
    private final EleveRepository eleveRepository = mock(EleveRepository.class);
    private final LienParentalRepository lienParentalRepository = mock(LienParentalRepository.class);
    private final PerimetreFinancesService perimetreFinances = mock(PerimetreFinancesService.class);

    private final DocumentsPdfService service = new DocumentsPdfService(mock(MoyennesService.class),
            mock(ClasseRepository.class), factureRepository, paiementRepository, eleveRepository,
            lienParentalRepository, perimetreFinances, new GenerateurPdf(),
            Clock.fixed(Instant.parse("2026-10-29T10:00:00Z"), ZoneOffset.UTC));

    @BeforeEach
    void preparer() {
        Eleve eleve = new Eleve();
        eleve.setId(ELEVE);
        eleve.setNom("Diallo");
        eleve.setPrenom("Awa");
        eleve.setMatricule("M-7");
        Etablissement etablissement = new Etablissement();
        etablissement.setNom("Collège Test");
        Facture facture = new Facture();
        facture.setId(FACTURE);
        facture.setNumero("FAC-1");
        facture.setLibelle("Scolarité");
        facture.setEleve(eleve);
        facture.setEtablissement(etablissement);
        facture.setMontantDu(new BigDecimal("100.00"));
        facture.setMontantExonere(BigDecimal.ZERO);
        facture.setDevise("EUR");
        when(factureRepository.findById(FACTURE)).thenReturn(Optional.of(facture));
        when(eleveRepository.findByUtilisateurId(PARENT)).thenReturn(Optional.empty());
    }

    private Paiement paiement(String statut) {
        Paiement p = new Paiement();
        p.setMontant(new BigDecimal("40.00"));
        p.setStatut(statut);
        p.setMode("ESPECES");
        p.setReferenceInterne("PAY-1");
        p.setCreatedAt(Instant.parse("2026-10-28T10:00:00Z"));
        return p;
    }

    @Test
    void unParentDeLElevePeutObtenirLeRecu() {
        when(lienParentalRepository.existsByEleveIdAndUtilisateurId(ELEVE, PARENT)).thenReturn(true);
        when(paiementRepository.findByFactureIdOrderByCreatedAtDescIdDesc(FACTURE))
                .thenReturn(List.of(paiement("VALIDE"), paiement("ECHEC")));

        byte[] pdf = service.recu(PARENT, FACTURE);

        assertThat(new String(pdf, 0, 5, java.nio.charset.StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
    }

    @Test
    void unEtrangerNeSaitMemePasQueLaFactureExiste() {
        when(lienParentalRepository.existsByEleveIdAndUtilisateurId(ELEVE, PARENT)).thenReturn(false);
        when(perimetreFinances.peutAccederFacture(FACTURE)).thenReturn(false);

        assertThatThrownBy(() -> service.recu(PARENT, FACTURE))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Facture introuvable");
    }

    @Test
    void sansPaiementValideIlNYAPasDeRecu() {
        when(perimetreFinances.peutAccederFacture(FACTURE)).thenReturn(true);
        when(paiementRepository.findByFactureIdOrderByCreatedAtDescIdDesc(FACTURE))
                .thenReturn(List.of(paiement("EN_ATTENTE")));

        assertThatThrownBy(() -> service.recu(PARENT, FACTURE))
                .hasMessageContaining("Aucun paiement validé");
    }
}
