package com.plateforme.scolaire.security;

import com.plateforme.scolaire.domain.Evaluation;
import com.plateforme.scolaire.repository.AffectationEnseignantRepository;
import com.plateforme.scolaire.repository.EvaluationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autorisations des évaluations et des notes, utilisables dans les annotations :
 * <pre>@PreAuthorize("@perimetreNotes.peutModifierEvaluation(#id)")</pre>
 *
 * <ul>
 *   <li><b>Écrire</b> : uniquement l'enseignant affecté à la classe pour la matière concernée.</li>
 *   <li><b>Lire</b> : mêmes règles que les présences (direction, enseignants de la classe, responsables régionaux
 *       et nationaux).</li>
 * </ul>
 */
@Service("perimetreNotes")
@RequiredArgsConstructor
public class PerimetreNotesService {

    private final PerimetreService perimetreService;
    private final EvaluationRepository evaluationRepository;
    private final AffectationEnseignantRepository affectationEnseignantRepository;

    /** Peut créer des évaluations dans cette classe (pour au moins une matière). */
    public boolean peutCreerEvaluation(Long classeId) {
        UtilisateurPrincipal p = perimetreService.utilisateurCourant();
        return p != null && classeId != null
                && affectationEnseignantRepository.existsByClasseIdAndEnseignantUtilisateurId(classeId, p.getId());
    }

    /** Enseigne cette matière dans cette classe. */
    public boolean peutEvaluerMatiere(Long classeId, Long matiereId) {
        UtilisateurPrincipal p = perimetreService.utilisateurCourant();
        return p != null && classeId != null && matiereId != null
                && affectationEnseignantRepository.existsByClasseIdAndMatiereIdAndEnseignantUtilisateurId(
                classeId, matiereId, p.getId());
    }

    public boolean peutConsulterClasse(Long classeId) {
        return perimetreService.peutConsulterPresences(classeId);
    }

    @Transactional(readOnly = true)
    public boolean peutConsulterEvaluation(Long evaluationId) {
        if (evaluationId == null) {
            return false;
        }
        return evaluationRepository.findClasseIdById(evaluationId)
                .map(perimetreService::peutConsulterPresences)
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean peutModifierEvaluation(Long evaluationId) {
        if (evaluationId == null) {
            return false;
        }
        Evaluation evaluation = evaluationRepository.findById(evaluationId).orElse(null);
        return evaluation != null
                && peutEvaluerMatiere(evaluation.getClasse().getId(), evaluation.getMatiere().getId());
    }
}
