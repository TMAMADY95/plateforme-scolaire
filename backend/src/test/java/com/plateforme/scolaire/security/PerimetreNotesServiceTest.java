package com.plateforme.scolaire.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.plateforme.scolaire.domain.Classe;
import com.plateforme.scolaire.domain.Evaluation;
import com.plateforme.scolaire.domain.Matiere;
import com.plateforme.scolaire.repository.AffectationEnseignantRepository;
import com.plateforme.scolaire.repository.EvaluationRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Écrire des notes : seulement l'enseignant de la classe ET de la matière. Lire : comme les présences. */
class PerimetreNotesServiceTest {

    private static final long UTILISATEUR = 7L;
    private static final long CLASSE = 5L;
    private static final long MATIERE = 3L;
    private static final long EVALUATION = 20L;

    private final PerimetreService perimetreService = mock(PerimetreService.class);
    private final EvaluationRepository evaluationRepository = mock(EvaluationRepository.class);
    private final AffectationEnseignantRepository affectations = mock(AffectationEnseignantRepository.class);
    private final PerimetreNotesService service =
            new PerimetreNotesService(perimetreService, evaluationRepository, affectations);

    @BeforeEach
    void preparer() {
        Classe classe = new Classe();
        classe.setId(CLASSE);
        Matiere matiere = new Matiere();
        matiere.setId(MATIERE);
        Evaluation evaluation = new Evaluation();
        evaluation.setId(EVALUATION);
        evaluation.setClasse(classe);
        evaluation.setMatiere(matiere);
        when(evaluationRepository.findById(EVALUATION)).thenReturn(Optional.of(evaluation));
        when(evaluationRepository.findClasseIdById(EVALUATION)).thenReturn(Optional.of(CLASSE));
    }

    private void connecter() {
        when(perimetreService.utilisateurCourant()).thenReturn(
                new UtilisateurPrincipal(UTILISATEUR, "u@example.org", "x", true, List.of()));
    }

    @Test
    void unEnseignantDeLaMatiereEtDeLaClassePeutEvaluerEtModifier() {
        connecter();
        when(affectations.existsByClasseIdAndEnseignantUtilisateurId(CLASSE, UTILISATEUR)).thenReturn(true);
        when(affectations.existsByClasseIdAndMatiereIdAndEnseignantUtilisateurId(CLASSE, MATIERE, UTILISATEUR))
                .thenReturn(true);

        assertThat(service.peutCreerEvaluation(CLASSE)).isTrue();
        assertThat(service.peutEvaluerMatiere(CLASSE, MATIERE)).isTrue();
        assertThat(service.peutModifierEvaluation(EVALUATION)).isTrue();
    }

    @Test
    void unEnseignantDUneAutreMatierePeutCreerDansLaClasseMaisPasNoterCetteMatiere() {
        connecter();
        when(affectations.existsByClasseIdAndEnseignantUtilisateurId(CLASSE, UTILISATEUR)).thenReturn(true);

        assertThat(service.peutCreerEvaluation(CLASSE)).isTrue();
        assertThat(service.peutEvaluerMatiere(CLASSE, MATIERE)).isFalse();
        assertThat(service.peutModifierEvaluation(EVALUATION)).isFalse();
    }

    @Test
    void laDirectionSansAffectationNePeutNiCreerNiModifier() {
        connecter();
        assertThat(service.peutCreerEvaluation(CLASSE)).isFalse();
        assertThat(service.peutModifierEvaluation(EVALUATION)).isFalse();
    }

    @Test
    void lAccesEnLectureSuitLesRegleDesPresences() {
        connecter();
        when(perimetreService.peutConsulterPresences(CLASSE)).thenReturn(true);

        assertThat(service.peutConsulterClasse(CLASSE)).isTrue();
        assertThat(service.peutConsulterEvaluation(EVALUATION)).isTrue();
        assertThat(service.peutConsulterClasse(99L)).isFalse();
    }

    @Test
    void uneEvaluationInexistanteEstRefusee() {
        connecter();
        assertThat(service.peutConsulterEvaluation(99L)).isFalse();
        assertThat(service.peutModifierEvaluation(99L)).isFalse();
    }

    @Test
    void unUtilisateurAnonymeNAAucunDroit() {
        assertThat(service.peutCreerEvaluation(CLASSE)).isFalse();
        assertThat(service.peutEvaluerMatiere(CLASSE, MATIERE)).isFalse();
        assertThat(service.peutModifierEvaluation(EVALUATION)).isFalse();
    }
}
