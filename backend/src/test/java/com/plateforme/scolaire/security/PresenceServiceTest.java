package com.plateforme.scolaire.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.plateforme.scolaire.domain.AnneeScolaire;
import com.plateforme.scolaire.domain.Classe;
import com.plateforme.scolaire.domain.Eleve;
import com.plateforme.scolaire.domain.Inscription;
import com.plateforme.scolaire.domain.Presence;
import com.plateforme.scolaire.repository.ClasseRepository;
import com.plateforme.scolaire.repository.InscriptionRepository;
import com.plateforme.scolaire.repository.PresenceRepository;
import com.plateforme.scolaire.repository.UtilisateurRepository;
import com.plateforme.scolaire.service.AuditService;
import com.plateforme.scolaire.service.PresenceService;
import com.plateforme.scolaire.web.dto.AppelRequest;
import com.plateforme.scolaire.web.dto.FeuilleAppelDto;
import com.plateforme.scolaire.web.dto.LignePresenceRequest;
import com.plateforme.scolaire.web.dto.StatutPresence;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

/** Règles métier de l'appel (sans base de données). "Aujourd'hui" est fixé au 26 octobre 2026. */
class PresenceServiceTest {

    private static final LocalDate AUJOURDHUI = LocalDate.of(2026, 10, 26);
    private static final long CLASSE = 5L;

    private final PresenceRepository presenceRepository = mock(PresenceRepository.class);
    private final InscriptionRepository inscriptionRepository = mock(InscriptionRepository.class);
    private final ClasseRepository classeRepository = mock(ClasseRepository.class);
    private final AuditService auditService = mock(AuditService.class);
    private final Clock horloge = Clock.fixed(Instant.parse("2026-10-26T10:00:00Z"), ZoneOffset.UTC);
    private final PresenceService service = new PresenceService(presenceRepository, inscriptionRepository,
            classeRepository, mock(UtilisateurRepository.class), mock(PerimetreService.class), horloge,
            auditService);

    private Inscription dupont;
    private Inscription martin;

    @BeforeEach
    void preparer() {
        AnneeScolaire annee = new AnneeScolaire();
        annee.setId(1L);
        annee.setDateDebut(LocalDate.of(2026, 9, 1));
        annee.setDateFin(LocalDate.of(2027, 6, 30));
        Classe classe = new Classe();
        classe.setId(CLASSE);
        classe.setAnneeScolaire(annee);
        dupont = inscription(12L, "Dupont", "Paul");
        martin = inscription(11L, "Martin", "Ines");

        when(classeRepository.findById(CLASSE)).thenReturn(Optional.of(classe));
        when(inscriptionRepository.findByClasseIdAndStatutOrderByEleveNomAscElevePrenomAsc(CLASSE, "ACCEPTEE"))
                .thenReturn(List.of(dupont, martin));
        when(presenceRepository.findByInscriptionClasseIdAndDateSeance(eq(CLASSE), any())).thenReturn(List.of());
        when(presenceRepository.save(any(Presence.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static Inscription inscription(long id, String nom, String prenom) {
        Eleve eleve = new Eleve();
        eleve.setNom(nom);
        eleve.setPrenom(prenom);
        Inscription i = new Inscription();
        i.setId(id);
        i.setEleve(eleve);
        return i;
    }

    private static AppelRequest appel(LignePresenceRequest... lignes) {
        return new AppelRequest(List.of(lignes));
    }

    private static void attendreStatut(ThrowingCallable appel, int statutHttp) {
        assertThatThrownBy(appel).isInstanceOfSatisfying(ResponseStatusException.class,
                e -> assertThat(e.getStatusCode().value()).isEqualTo(statutHttp));
    }

    // ---------- Feuille d'appel ----------

    @Test
    void laFeuilleListeTousLesElevesAcceptesSansStatutTantQueLAppelNEstPasFait() {
        FeuilleAppelDto feuille = service.feuille(CLASSE, AUJOURDHUI);
        assertThat(feuille.lignes()).hasSize(2);
        assertThat(feuille.lignes().get(0).eleve().nom()).isEqualTo("Dupont");
        assertThat(feuille.lignes()).allSatisfy(l -> assertThat(l.statut()).isNull());
    }

    @Test
    void sansDateLaFeuilleEstCelleDAujourdhui() {
        assertThat(service.feuille(CLASSE, null).date()).isEqualTo(AUJOURDHUI);
    }

    @Test
    void uneClasseInconnueDonne404() {
        attendreStatut(() -> service.feuille(99L, AUJOURDHUI), 404);
    }

    // ---------- Saisie ----------

    @Test
    void lAppelEnregistreLesPresencesEtIgnoreLeMotifDUnEleveePresent() {
        FeuilleAppelDto feuille = service.enregistrer(CLASSE, AUJOURDHUI, appel(
                new LignePresenceRequest(12L, StatutPresence.PRESENT, "ignoré", true),
                new LignePresenceRequest(11L, StatutPresence.ABSENT, " Maladie ", true)));

        assertThat(feuille.lignes().get(0).statut()).isEqualTo(StatutPresence.PRESENT);
        assertThat(feuille.lignes().get(0).motif()).isNull();
        assertThat(feuille.lignes().get(0).justifie()).isFalse();
        assertThat(feuille.lignes().get(1).statut()).isEqualTo(StatutPresence.ABSENT);
        assertThat(feuille.lignes().get(1).motif()).isEqualTo("Maladie");
        assertThat(feuille.lignes().get(1).justifie()).isTrue();
        verify(presenceRepository, times(2)).save(any(Presence.class));
    }

    @Test
    void unNouvelAppelEstJournalise() {
        service.enregistrer(CLASSE, AUJOURDHUI, appel(
                new LignePresenceRequest(12L, StatutPresence.PRESENT, null, null),
                new LignePresenceRequest(11L, StatutPresence.ABSENT, null, null)));

        verify(auditService).enregistrer(eq("SAISIE_APPEL"), eq("classe"), eq(CLASSE), any());
        verify(auditService, never()).enregistrer(eq("CORRECTION_PRESENCE"), any(), any(), any());
    }

    @Test
    void renvoyerLeMemeAppelNeLaisseAucuneTrace() {
        Presence existante = new Presence();
        existante.setInscription(martin);
        existante.setDateSeance(AUJOURDHUI);
        existante.setStatut("ABSENT");
        when(presenceRepository.findByInscriptionClasseIdAndDateSeance(CLASSE, AUJOURDHUI))
                .thenReturn(List.of(existante));

        service.enregistrer(CLASSE, AUJOURDHUI, appel(new LignePresenceRequest(11L, StatutPresence.ABSENT, null, null)));

        verify(auditService, never()).enregistrer(any(), any(), any(), any());
    }

    @Test
    void corrigerUneAbsenceJournaliseLAvantEtLApres() {
        Presence existante = new Presence();
        existante.setId(40L);
        existante.setInscription(martin);
        existante.setDateSeance(AUJOURDHUI);
        existante.setStatut("ABSENT");
        when(presenceRepository.findByInscriptionClasseIdAndDateSeance(CLASSE, AUJOURDHUI))
                .thenReturn(List.of(existante));

        service.enregistrer(CLASSE, AUJOURDHUI, appel(new LignePresenceRequest(11L, StatutPresence.PRESENT, null, null)));

        verify(auditService).enregistrer(eq("CORRECTION_PRESENCE"), eq("presence"), eq(40L), any());
        verify(auditService).enregistrer(eq("SAISIE_APPEL"), eq("classe"), eq(CLASSE), any());
    }

    @Test
    void unAppelPeutCorrigerUneSeulePresenceSansToucherAuxAutres() {
        Presence existante = new Presence();
        existante.setInscription(martin);
        existante.setDateSeance(AUJOURDHUI);
        existante.setStatut("ABSENT");
        when(presenceRepository.findByInscriptionClasseIdAndDateSeance(CLASSE, AUJOURDHUI))
                .thenReturn(List.of(existante));

        FeuilleAppelDto feuille = service.enregistrer(CLASSE, AUJOURDHUI, appel(
                new LignePresenceRequest(11L, StatutPresence.ABSENT, "Certificat médical", true)));

        assertThat(existante.isJustifie()).isTrue();
        assertThat(existante.getMotif()).isEqualTo("Certificat médical");
        assertThat(feuille.lignes().get(0).statut()).isNull(); // Dupont : appel non fait
        verify(presenceRepository, times(1)).save(any(Presence.class));
    }

    @Test
    void onNePeutPasSaisirUnePresenceDansLeFutur() {
        attendreStatut(() -> service.enregistrer(CLASSE, AUJOURDHUI.plusDays(1), appel(
                new LignePresenceRequest(12L, StatutPresence.PRESENT, null, null))), 400);
    }

    @Test
    void onNePeutPasSaisirUnePresenceHorsDeLAnneeScolaire() {
        attendreStatut(() -> service.enregistrer(CLASSE, LocalDate.of(2026, 8, 1), appel(
                new LignePresenceRequest(12L, StatutPresence.PRESENT, null, null))), 400);
    }

    @Test
    void uneInscriptionQuiNEstPasDeLaClasseEstRefusee() {
        attendreStatut(() -> service.enregistrer(CLASSE, AUJOURDHUI, appel(
                new LignePresenceRequest(99L, StatutPresence.PRESENT, null, null))), 400);
    }

    @Test
    void unEleveNePeutPasApparaitreDeuxFoisDansLaMemeRequete() {
        attendreStatut(() -> service.enregistrer(CLASSE, AUJOURDHUI, appel(
                new LignePresenceRequest(12L, StatutPresence.PRESENT, null, null),
                new LignePresenceRequest(12L, StatutPresence.ABSENT, null, null))), 400);
    }

    // ---------- Historique d'un élève ----------

    @Test
    void lHistoriqueRefuseUnePeriodeInversee() {
        attendreStatut(() -> service.historiqueEleve(50L, AUJOURDHUI, AUJOURDHUI.minusDays(1)), 400);
    }

    @Test
    void lHistoriqueRefuseUnePeriodeDePlusDUnAn() {
        attendreStatut(() -> service.historiqueEleve(50L, AUJOURDHUI.minusDays(400), AUJOURDHUI), 400);
    }

    @Test
    void lHistoriqueParDefautCouvreLesTrenteDerniersJours() {
        when(presenceRepository.findByInscriptionEleveIdAndDateSeanceBetweenOrderByDateSeanceDesc(
                50L, AUJOURDHUI.minusDays(30), AUJOURDHUI)).thenReturn(List.of());
        assertThat(service.historiqueEleve(50L, null, null)).isEmpty();
    }
}
