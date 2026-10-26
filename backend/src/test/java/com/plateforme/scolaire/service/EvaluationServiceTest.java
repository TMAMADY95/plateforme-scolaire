package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.plateforme.scolaire.domain.AnneeScolaire;
import com.plateforme.scolaire.domain.Classe;
import com.plateforme.scolaire.domain.Eleve;
import com.plateforme.scolaire.domain.Enseignant;
import com.plateforme.scolaire.domain.Evaluation;
import com.plateforme.scolaire.domain.Inscription;
import com.plateforme.scolaire.domain.Matiere;
import com.plateforme.scolaire.domain.Note;
import com.plateforme.scolaire.domain.Periode;
import com.plateforme.scolaire.repository.ClasseRepository;
import com.plateforme.scolaire.repository.EnseignantRepository;
import com.plateforme.scolaire.repository.EvaluationRepository;
import com.plateforme.scolaire.repository.InscriptionRepository;
import com.plateforme.scolaire.repository.MatiereRepository;
import com.plateforme.scolaire.repository.NoteRepository;
import com.plateforme.scolaire.repository.PeriodeRepository;
import com.plateforme.scolaire.repository.UtilisateurRepository;
import com.plateforme.scolaire.security.PerimetreNotesService;
import com.plateforme.scolaire.security.PerimetreService;
import com.plateforme.scolaire.security.UtilisateurPrincipal;
import com.plateforme.scolaire.web.dto.EvaluationModification;
import com.plateforme.scolaire.web.dto.EvaluationRequest;
import com.plateforme.scolaire.web.dto.GrilleNotesDto;
import com.plateforme.scolaire.web.dto.LigneNoteRequest;
import com.plateforme.scolaire.web.dto.NotesRequest;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

/** Règles métier des évaluations et des notes (sans base de données). Barème de l'évaluation : 20. */
class EvaluationServiceTest {

    private static final long CLASSE = 5L;
    private static final long EVALUATION = 20L;

    private final EvaluationRepository evaluationRepository = mock(EvaluationRepository.class);
    private final NoteRepository noteRepository = mock(NoteRepository.class);
    private final ClasseRepository classeRepository = mock(ClasseRepository.class);
    private final MatiereRepository matiereRepository = mock(MatiereRepository.class);
    private final PeriodeRepository periodeRepository = mock(PeriodeRepository.class);
    private final EnseignantRepository enseignantRepository = mock(EnseignantRepository.class);
    private final InscriptionRepository inscriptionRepository = mock(InscriptionRepository.class);
    private final AuditService auditService = mock(AuditService.class);
    private final PerimetreService perimetreService = mock(PerimetreService.class);
    private final PerimetreNotesService perimetreNotesService = mock(PerimetreNotesService.class);
    private final Clock horloge = Clock.fixed(Instant.parse("2026-10-26T10:00:00Z"), ZoneOffset.UTC);
    private final EvaluationService service = new EvaluationService(evaluationRepository, noteRepository,
            classeRepository, matiereRepository, periodeRepository, enseignantRepository, inscriptionRepository,
            mock(UtilisateurRepository.class), auditService, perimetreService, perimetreNotesService, horloge);

    private AnneeScolaire annee;
    private Classe classe;
    private Matiere matiere;
    private Periode periode;
    private Evaluation evaluation;
    private Inscription dupont;
    private Inscription martin;

    @BeforeEach
    void preparer() {
        annee = new AnneeScolaire();
        annee.setId(1L);
        classe = new Classe();
        classe.setId(CLASSE);
        classe.setAnneeScolaire(annee);
        matiere = new Matiere();
        matiere.setId(3L);
        periode = new Periode();
        periode.setId(2L);
        periode.setAnneeScolaire(annee);
        periode.setDateDebut(LocalDate.of(2026, 9, 1));
        periode.setDateFin(LocalDate.of(2026, 12, 20));
        Enseignant enseignant = new Enseignant();
        enseignant.setId(9L);

        evaluation = new Evaluation();
        evaluation.setId(EVALUATION);
        evaluation.setClasse(classe);
        evaluation.setMatiere(matiere);
        evaluation.setPeriode(periode);
        evaluation.setEnseignant(enseignant);
        evaluation.setTitre("Contrôle 1");
        evaluation.setDateEvaluation(LocalDate.of(2026, 10, 20));
        evaluation.setBareme(BigDecimal.valueOf(20));
        evaluation.setCoefficient(BigDecimal.ONE);

        dupont = inscription(12L, "Dupont");
        martin = inscription(11L, "Martin");

        when(classeRepository.findById(CLASSE)).thenReturn(Optional.of(classe));
        when(matiereRepository.findById(3L)).thenReturn(Optional.of(matiere));
        when(periodeRepository.findById(2L)).thenReturn(Optional.of(periode));
        when(perimetreService.utilisateurCourant())
                .thenReturn(new UtilisateurPrincipal(7L, "prof@example.org", "x", true, List.of()));
        when(enseignantRepository.findByUtilisateurId(7L)).thenReturn(Optional.of(enseignant));
        when(perimetreNotesService.peutEvaluerMatiere(CLASSE, 3L)).thenReturn(true);
        when(evaluationRepository.findById(EVALUATION)).thenReturn(Optional.of(evaluation));
        when(evaluationRepository.save(any(Evaluation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(inscriptionRepository.findByClasseIdAndStatutOrderByEleveNomAscElevePrenomAsc(CLASSE, "ACCEPTEE"))
                .thenReturn(List.of(dupont, martin));
        when(noteRepository.findByEvaluationId(EVALUATION)).thenReturn(List.of());
        when(noteRepository.save(any(Note.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ---------- Aides ----------

    private static Inscription inscription(long id, String nom) {
        Eleve eleve = new Eleve();
        eleve.setNom(nom);
        Inscription i = new Inscription();
        i.setId(id);
        i.setEleve(eleve);
        return i;
    }

    private Note noteExistante(Inscription inscription, String valeur) {
        Note n = new Note();
        n.setId(100L);
        n.setEvaluation(evaluation);
        n.setInscription(inscription);
        n.setValeur(valeur == null ? null : new BigDecimal(valeur));
        when(noteRepository.findByEvaluationId(EVALUATION)).thenReturn(List.of(n));
        return n;
    }

    private static NotesRequest notes(LigneNoteRequest... lignes) {
        return new NotesRequest(List.of(lignes));
    }

    private static void attendreStatut(ThrowingCallable appel, int statutHttp) {
        assertThatThrownBy(appel).isInstanceOfSatisfying(ResponseStatusException.class,
                e -> assertThat(e.getStatusCode().value()).isEqualTo(statutHttp));
    }

    // ---------- Création d'une évaluation ----------

    @Test
    void uneEvaluationPrendUnCoefficientDeUnEtUnBaremeDeVingtParDefaut() {
        var dto = service.creer(CLASSE, new EvaluationRequest(3L, 2L, " Contrôle 1 ",
                LocalDate.of(2026, 10, 20), null, null));
        assertThat(dto.coefficient()).isEqualByComparingTo("1");
        assertThat(dto.bareme()).isEqualByComparingTo("20");
        assertThat(dto.titre()).isEqualTo("Contrôle 1");
        assertThat(dto.enseignantId()).isEqualTo(9L);
    }

    @Test
    void unEnseignantNeCreePasUneEvaluationDansUneMatiereQuIlNEnseignePas() {
        when(perimetreNotesService.peutEvaluerMatiere(CLASSE, 3L)).thenReturn(false);
        assertThatThrownBy(() -> service.creer(CLASSE, new EvaluationRequest(3L, 2L, "X",
                LocalDate.of(2026, 10, 20), null, null))).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void laPeriodeDoitAppartenirALAnneeDeLaClasse() {
        AnneeScolaire autre = new AnneeScolaire();
        autre.setId(2L);
        periode.setAnneeScolaire(autre);
        attendreStatut(() -> service.creer(CLASSE, new EvaluationRequest(3L, 2L, "X",
                LocalDate.of(2026, 10, 20), null, null)), 400);
    }

    @Test
    void laDateDoitEtreDansLaPeriode() {
        attendreStatut(() -> service.creer(CLASSE, new EvaluationRequest(3L, 2L, "X",
                LocalDate.of(2027, 1, 10), null, null)), 400);
    }

    @Test
    void leBaremeNePeutPasPasserSousUneNoteDejaSaisie() {
        when(noteRepository.maxValeur(EVALUATION)).thenReturn(new BigDecimal("18.00"));
        attendreStatut(() -> service.modifier(EVALUATION, new EvaluationModification("Contrôle 1",
                LocalDate.of(2026, 10, 20), null, new BigDecimal("15"))), 409);
    }

    // ---------- Saisie des notes ----------

    @Test
    void uneNoteNePeutPasDepasserLeBareme() {
        attendreStatut(() -> service.saisirNotes(EVALUATION, notes(
                new LigneNoteRequest(12L, new BigDecimal("20.5"), false))), 400);
    }

    @Test
    void unEleveAbsentNAPasDeNote() {
        attendreStatut(() -> service.saisirNotes(EVALUATION, notes(
                new LigneNoteRequest(12L, new BigDecimal("10"), true))), 400);
    }

    @Test
    void uneInscriptionEtrangereALaClasseEstRefusee() {
        attendreStatut(() -> service.saisirNotes(EVALUATION, notes(
                new LigneNoteRequest(99L, new BigDecimal("10"), false))), 400);
    }

    @Test
    void unEleveNePeutPasApparaitreDeuxFois() {
        attendreStatut(() -> service.saisirNotes(EVALUATION, notes(
                new LigneNoteRequest(12L, new BigDecimal("10"), false),
                new LigneNoteRequest(12L, new BigDecimal("11"), false))), 400);
    }

    @Test
    void uneErreurDansLaRequeteNEcritRienDuTout() {
        attendreStatut(() -> service.saisirNotes(EVALUATION, notes(
                new LigneNoteRequest(12L, new BigDecimal("10"), false),
                new LigneNoteRequest(11L, new BigDecimal("25"), false))), 400);
        verify(noteRepository, never()).save(any(Note.class));
    }

    @Test
    void uneNouvelleNoteEstEnregistreeSansAudit() {
        GrilleNotesDto grille = service.saisirNotes(EVALUATION, notes(
                new LigneNoteRequest(12L, new BigDecimal("14"), false)));

        assertThat(grille.lignes().get(0).valeur()).isEqualByComparingTo("14.00");
        assertThat(grille.lignes().get(1).valeur()).isNull();
        verify(auditService, never()).enregistrer(any(), any(), any(), any());
    }

    @Test
    void uneAbsenceEstEnregistreeSansValeur() {
        GrilleNotesDto grille = service.saisirNotes(EVALUATION, notes(new LigneNoteRequest(12L, null, true)));
        assertThat(grille.lignes().get(0).absent()).isTrue();
        assertThat(grille.lignes().get(0).valeur()).isNull();
    }

    @Test
    void modifierUneNoteExistanteEstJournalise() {
        Note existante = noteExistante(dupont, "12.00");

        service.saisirNotes(EVALUATION, notes(new LigneNoteRequest(12L, new BigDecimal("15"), false)));

        assertThat(existante.getValeur()).isEqualByComparingTo("15.00");
        verify(auditService).enregistrer(eq("MODIFICATION_NOTE"), eq("Note"), eq(100L), any());
    }

    @Test
    void resaisirLaMemeNoteNEstPasJournalise() {
        noteExistante(dupont, "12.00");
        service.saisirNotes(EVALUATION, notes(new LigneNoteRequest(12L, new BigDecimal("12"), false)));
        verify(auditService, never()).enregistrer(any(), any(), any(), any());
    }

    @Test
    void effacerUneNoteLaSupprimeEtLeJournalise() {
        Note existante = noteExistante(dupont, "12.00");

        GrilleNotesDto grille = service.saisirNotes(EVALUATION, notes(new LigneNoteRequest(12L, null, false)));

        verify(noteRepository).delete(existante);
        verify(auditService).enregistrer(eq("SUPPRESSION_NOTE"), eq("Note"), eq(100L), any());
        assertThat(grille.lignes().get(0).valeur()).isNull();
    }
}
