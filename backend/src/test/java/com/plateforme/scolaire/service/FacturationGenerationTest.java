package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.plateforme.scolaire.domain.AnneeScolaire;
import com.plateforme.scolaire.domain.Classe;
import com.plateforme.scolaire.domain.Eleve;
import com.plateforme.scolaire.domain.Etablissement;
import com.plateforme.scolaire.domain.Facture;
import com.plateforme.scolaire.domain.Frais;
import com.plateforme.scolaire.domain.Inscription;
import com.plateforme.scolaire.domain.Niveau;
import com.plateforme.scolaire.repository.AnneeScolaireRepository;
import com.plateforme.scolaire.repository.ClasseRepository;
import com.plateforme.scolaire.repository.EtablissementRepository;
import com.plateforme.scolaire.repository.ExonerationRepository;
import com.plateforme.scolaire.repository.FactureRepository;
import com.plateforme.scolaire.repository.FraisRepository;
import com.plateforme.scolaire.repository.InscriptionRepository;
import com.plateforme.scolaire.repository.NiveauRepository;
import com.plateforme.scolaire.repository.UtilisateurRepository;
import com.plateforme.scolaire.security.PerimetreService;
import com.plateforme.scolaire.web.dto.ExonerationRequest;
import com.plateforme.scolaire.web.dto.GenererFacturesRequest;
import com.plateforme.scolaire.web.dto.ResultatGenerationDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

/** Génération des factures : chaque élève n'est facturé que pour son propre niveau. */
class FacturationGenerationTest {

    private static final long ETABLISSEMENT = 1L;
    private static final long ANNEE = 2L;
    private static final long NIVEAU_6E = 10L;
    private static final long NIVEAU_5E = 11L;

    private final FactureRepository factureRepository = mock(FactureRepository.class);
    private final FraisRepository fraisRepository = mock(FraisRepository.class);
    private final EtablissementRepository etablissementRepository = mock(EtablissementRepository.class);
    private final AnneeScolaireRepository anneeRepository = mock(AnneeScolaireRepository.class);
    private final NiveauRepository niveauRepository = mock(NiveauRepository.class);
    private final InscriptionRepository inscriptionRepository = mock(InscriptionRepository.class);

    private final FacturationService service = new FacturationService(
            factureRepository, fraisRepository, mock(ExonerationRepository.class), etablissementRepository,
            anneeRepository, mock(ClasseRepository.class), niveauRepository, inscriptionRepository,
            mock(UtilisateurRepository.class), mock(PerimetreService.class), mock(AuditService.class),
            mock(com.plateforme.scolaire.repository.PaiementRepository.class));

    @BeforeEach
    void preparer() {
        Etablissement etablissement = new Etablissement();
        etablissement.setId(ETABLISSEMENT);
        AnneeScolaire annee = new AnneeScolaire();
        annee.setId(ANNEE);
        annee.setLibelle("2026-2027");
        when(etablissementRepository.findById(ETABLISSEMENT)).thenReturn(Optional.of(etablissement));
        when(anneeRepository.findById(ANNEE)).thenReturn(Optional.of(annee));
        when(niveauRepository.findById(NIVEAU_6E)).thenReturn(Optional.of(niveau(NIVEAU_6E)));

        Frais scolarite = new Frais();
        scolarite.setId(50L);
        scolarite.setLibelle("Scolarité");
        scolarite.setMontant(new BigDecimal("1200.00"));
        scolarite.setDevise("EUR");
        when(fraisRepository.findApplicables(ETABLISSEMENT, ANNEE, NIVEAU_6E)).thenReturn(List.of(scolarite));

        // Numérotation : le compteur est une table lue en SQL natif.
        EntityManager em = mock(EntityManager.class);
        Query requete = mock(Query.class);
        when(em.createNativeQuery(anyString())).thenReturn(requete);
        when(requete.setParameter(anyInt(), any())).thenReturn(requete);
        when(requete.getResultList()).thenReturn(List.of());
        when(requete.executeUpdate()).thenReturn(1);
        ReflectionTestUtils.setField(service, "entityManager", em);
    }

    private static Niveau niveau(long id) {
        Niveau n = new Niveau();
        n.setId(id);
        return n;
    }

    private static Inscription inscription(long eleveId, long niveauId) {
        Eleve eleve = new Eleve();
        eleve.setId(eleveId);
        Classe classe = new Classe();
        classe.setNiveau(niveau(niveauId));
        Inscription i = new Inscription();
        i.setEleve(eleve);
        i.setClasse(classe);
        i.setStatut("ACCEPTEE");
        return i;
    }

    @Test
    void laGenerationParNiveauNeFactureQueLesElevesDeCeNiveau() {
        when(inscriptionRepository.findByEtablissementIdAndAnneeScolaireIdOrderByDateDemandeDescIdDesc(
                ETABLISSEMENT, ANNEE))
                .thenReturn(List.of(inscription(100L, NIVEAU_6E), inscription(101L, NIVEAU_5E)));

        ResultatGenerationDto resultat =
                service.generer(ETABLISSEMENT, new GenererFacturesRequest(ANNEE, null, NIVEAU_6E));

        ArgumentCaptor<Facture> captor = ArgumentCaptor.forClass(Facture.class);
        verify(factureRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getEleve().getId()).isEqualTo(100L);
        assertThat(captor.getValue().getDevise()).isEqualTo("EUR");
        assertThat(resultat.facturesCreees()).isEqualTo(1);
        assertThat(resultat.elevesTraites()).isEqualTo(1);
        assertThat(resultat.montantTotalEmis()).isEqualByComparingTo("1200.00");
    }

    @Test
    void relancerLaGenerationNeCreePasDeDoublon() {
        when(inscriptionRepository.findByEtablissementIdAndAnneeScolaireIdOrderByDateDemandeDescIdDesc(
                ETABLISSEMENT, ANNEE)).thenReturn(List.of(inscription(100L, NIVEAU_6E)));
        when(factureRepository.existsByFraisIdAndEleveId(50L, 100L)).thenReturn(true);

        ResultatGenerationDto resultat =
                service.generer(ETABLISSEMENT, new GenererFacturesRequest(ANNEE, null, NIVEAU_6E));

        verify(factureRepository, never()).save(any(Facture.class));
        assertThat(resultat.facturesCreees()).isZero();
        assertThat(resultat.facturesIgnorees()).isEqualTo(1);
    }

    @Test
    void unTypeDExonerationInconnuEstRefuse() {
        Facture facture = new Facture();
        facture.setId(7L);
        facture.setMontantDu(new BigDecimal("100.00"));
        facture.setStatut("IMPAYEE");
        when(factureRepository.findById(7L)).thenReturn(Optional.of(facture));

        assertThatThrownBy(() -> service.exonerer(7L,
                new ExonerationRequest(new BigDecimal("10.00"), "Motif", "CADEAU")))
                .isInstanceOf(ResponseStatusException.class);
    }
}
