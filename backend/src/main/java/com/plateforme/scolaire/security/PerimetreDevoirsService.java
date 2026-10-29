package com.plateforme.scolaire.security;

import com.plateforme.scolaire.domain.Devoir;
import com.plateforme.scolaire.repository.DevoirRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autorisations des devoirs : publier = enseigner la matière dans la classe (voir {@link PerimetreNotesService});
 * modifier ou supprimer = enseigner cette matière dans la classe, ou diriger l'établissement ; consulter = comme
 * l'emploi du temps de la classe (équipe, élèves de la classe et leurs parents).
 */
@Service("perimetreDevoirs")
@RequiredArgsConstructor
public class PerimetreDevoirsService {

    private final PerimetreService perimetreService;
    private final PerimetreNotesService perimetreNotesService;
    private final DevoirRepository devoirRepository;

    @Transactional(readOnly = true)
    public boolean peutModifier(Long devoirId) {
        if (devoirId == null) {
            return false;
        }
        Devoir devoir = devoirRepository.findById(devoirId).orElse(null);
        if (devoir == null) {
            return false;
        }
        Long classeId = devoir.getClasse().getId();
        return perimetreNotesService.peutEvaluerMatiere(classeId, devoir.getMatiere().getId())
                || perimetreService.peutGererClasse(classeId);
    }
}
