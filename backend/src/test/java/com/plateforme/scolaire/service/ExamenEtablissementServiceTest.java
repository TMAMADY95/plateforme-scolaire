package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.plateforme.scolaire.domain.AnneeScolaire;
import com.plateforme.scolaire.domain.Classe;
import com.plateforme.scolaire.domain.Eleve;
import com.plateforme.scolaire.domain.Etablissement;
import com.plateforme.scolaire.domain.ExamenNational;
import com.plateforme.scolaire.domain.Inscription;
import com.plateforme.scolaire.domain.Niveau;
import com.plateforme.scolaire.repository.CandidatExamenRepository;
import com.plateforme.scolaire.repository.CentreExamenRepository;
import com.plateforme.scolaire.repository.EtablissementRepository;
import com.plateforme.scolaire.repository.ExamenNationalRepository;
import com.plateforme.scolaire.repository.InscriptionRepository;
import com.plateforme.scolaire.repository.ZoneGeographiqueRepository;
import com.plateforme.scolaire.security.PerimetreService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class ExamenEtablissementServiceTest {

    private final ExamenNationalRepository examens = mock(ExamenNationalRepository.class);
    private final CandidatExamenRepository candidats = mock(CandidatExamenRepository.class);
    private final InscriptionRepository inscriptions = mock(InscriptionRepository.class);
    private final EtablissementRepository etablissements = mock(EtablissementRepository.class);
    private final ExamenEtablissementService service = new ExamenEtablissementService(examens, candidats,
            mock(CentreExamenRepository.class), inscriptions, etablissements, mock(ZoneGeographiqueRepository.class),
            mock(PerimetreService.class), mock(com.plateforme.scolaire.repository.EpreuveExamenRepository.class),
            new GenerateurPdf(), mock(AuditService.class),
            Clock.fixed(Instant.parse("2026-06-01T08:00:00Z"), ZoneOffset.UTC),
            mock(CentresExamenService.class), mock(com.plateforme.scolaire.repository.PresenceRepository.class),
            mock(com.plateforme.scolaire.repository.ListeCandidatsRepository.class));

    private ExamenNational examenPourNiveau(Niveau niveau) {
        AnneeScolaire annee = new AnneeScolaire();
        annee.setId(1L);
        ExamenNational e = new ExamenNational();
        e.setId(7L);
        e.setLibelle("BEPC");
        e.setAnneeScolaire(annee);
        e.setNiveau(niveau);
        when(examens.findById(7L)).thenReturn(java.util.Optional.of(e));
        Etablissement etab = new Etablissement();
        etab.setId(3L);
        when(etablissements.findById(3L)).thenReturn(java.util.Optional.of(etab));
        return e;
    }

    private static Niveau niveau(long id) {
        Niveau n = new Niveau();
        n.setId(id);
        return n;
    }

    private Inscription inscription(long eleveId, Niveau niveau, String statut) {
        Eleve eleve = new Eleve();
        eleve.setId(eleveId);
        eleve.setNom("Diallo");
        eleve.setPrenom("Aminata");
        Classe classe = new Classe();
        classe.setNom("10e A");
        classe.setNiveau(niveau);
        Inscription i = new Inscription();
        i.setEleve(eleve);
        i.setClasse(classe);
        i.setStatut(statut);
        return i;
    }

    @Test
    void unEleveHorsDuNiveauDeLExamenNEstPasInscrit() {
        examenPourNiveau(niveau(10));
        when(inscriptions.findByEtablissementIdAndAnneeScolaireIdOrderByDateDemandeDescIdDesc(3L, 1L))
                .thenReturn(List.of(inscription(5L, niveau(9), "ACCEPTEE")));

        assertThatThrownBy(() -> service.inscrire(3L, 7L, List.of(5L))).isInstanceOf(ResponseStatusException.class);
        verify(candidats, never()).save(any());
    }

    @Test
    void uneInscriptionNonAccepteeNeFaitPasUnCandidat() {
        examenPourNiveau(niveau(10));
        when(inscriptions.findByEtablissementIdAndAnneeScolaireIdOrderByDateDemandeDescIdDesc(3L, 1L))
                .thenReturn(List.of(inscription(5L, niveau(10), "EN_ATTENTE")));

        assertThatThrownBy(() -> service.inscrire(3L, 7L, List.of(5L))).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void leNumeroDeCandidatContientLExamenEtLIdentifiant() {
        ExamenNational e = new ExamenNational();
        e.setId(7L);
        assertThat(ExamenEtablissementService.numero(e, 42L)).isEqualTo("EX7-000042");
    }

    @Test
    void lesConvocationsProduisentUnPdfMemeSansCentre() {
        var pdf = new GenerateurPdf().convocations(new GenerateurPdf.DonneesConvocations("École A", "BEPC", "2025-2026", null,
                List.of(new GenerateurPdf.Convocation("Aminata Diallo", "M1", "EX7-000042", "10e A", null, null, null))));
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
    }

    private static com.plateforme.scolaire.domain.ZoneGeographique zone(long id, String type, com.plateforme.scolaire.domain.ZoneGeographique parent) {
        var z = new com.plateforme.scolaire.domain.ZoneGeographique();
        z.setId(id);
        z.setNom("Zone " + id);
        z.setTypeZone(type);
        z.setParent(parent);
        return z;
    }

    private static Etablissement ecoleDans(com.plateforme.scolaire.domain.ZoneGeographique z) {
        Etablissement e = new Etablissement();
        e.setZone(z);
        return e;
    }

    private static com.plateforme.scolaire.domain.CentreExamen centre(long id, Etablissement siege) {
        var c = new com.plateforme.scolaire.domain.CentreExamen();
        c.setId(id);
        c.setNom("Centre " + id);
        c.setEtablissement(siege);
        return c;
    }

    @Test
    void leCentreEstChoisiDansLaCommuneDeLEtablissementLeMoinsCharge() {
        var prefecture = zone(1, "PREFECTURE", null);
        var communeA = zone(2, "COMMUNE", prefecture);
        var communeB = zone(3, "COMMUNE", prefecture);
        var quartier = zone(4, "QUARTIER", communeA);   // une école de quartier relève de sa commune
        var centreA1 = centre(10, ecoleDans(communeA));
        var centreA2 = centre(11, ecoleDans(quartier));
        var centreB = centre(12, ecoleDans(communeB));
        var charges = new java.util.HashMap<Long, Long>(java.util.Map.of(10L, 30L, 11L, 5L, 12L, 0L));

        var choisi = ExamenEtablissementService.centreDeLaCommune(ecoleDans(quartier), List.of(centreA1, centreA2, centreB), charges);

        assertThat(choisi).isSameAs(centreA2);            // même commune, moins chargé : jamais le centre de l'autre commune
    }

    @Test
    void sansCentreDansLaCommuneAucunCentreNEstChoisi() {
        var communeA = zone(2, "COMMUNE", null);
        var communeB = zone(3, "COMMUNE", null);
        var centreB = centre(12, ecoleDans(communeB));

        assertThat(ExamenEtablissementService.centreDeLaCommune(ecoleDans(communeA), List.of(centreB), new java.util.HashMap<>())).isNull();
    }

    @Test
    void unCentreCompletEstIgnoreEtLeSuivantEstChoisi() {
        var commune = zone(2, "COMMUNE", null);
        var plein = centre(10, ecoleDans(commune));
        var libre = centre(11, ecoleDans(commune));
        var charges = new java.util.HashMap<Long, Long>(java.util.Map.of(10L, 80L, 11L, 40L));
        var capacites = java.util.Map.of(10L, 90L, 11L, 90L);   // 3 salles de 30

        // 20 candidats de plus : le centre 10 (80/90) déborderait, le centre 11 (40/90) convient
        assertThat(ExamenEtablissementService.centreDeLaCommune(ecoleDans(commune), List.of(plein, libre), charges, capacites, 20))
                .isSameAs(libre);
        // 60 candidats : aucun des deux ne tient
        assertThat(ExamenEtablissementService.centreDeLaCommune(ecoleDans(commune), List.of(plein, libre), charges, capacites, 60))
                .isNull();
    }

    @Test
    void lesAbsencesBloquentLaCandidatureAPartirDeVingtCinqPourCent() {
        assertThat(ExamenEtablissementService.depasseSeuil(null)).isFalse();             // aucune présence saisie
        assertThat(ExamenEtablissementService.depasseSeuil(new long[] {100, 24})).isFalse();
        assertThat(ExamenEtablissementService.depasseSeuil(new long[] {100, 25})).isTrue();
        assertThat(ExamenEtablissementService.depasseSeuil(new long[] {8, 2})).isTrue();   // exactement 25 %
    }

    @Test
    void leNumeroDePvIdentifieLExamenLeCentreEtLeRang() {
        assertThat(ExamenEtablissementService.numeroPv(7L, 12L, 31)).isEqualTo("PV7-12-0031");
    }
}
