package com.plateforme.scolaire.security;

import com.plateforme.scolaire.repository.CreneauRepository;
import com.plateforme.scolaire.repository.EleveRepository;
import com.plateforme.scolaire.repository.InscriptionRepository;
import com.plateforme.scolaire.repository.LienParentalRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autorisations des emplois du temps, utilisables dans les annotations :
 * <pre>@PreAuthorize("@perimetreEmploiDuTemps.peutConsulterClasse(#classeId)")</pre>
 *
 * <p>Même découpage que {@link PerimetreNotesService} : les règles propres aux emplois du temps
 * ne chargent pas {@link PerimetreService} de dépendances supplémentaires, ce qui laisse ses
 * tests unitaires intacts.</p>
 *
 * <ul>
 *   <li><b>Gérer</b> : direction ou personnel administratif de l'établissement, ou enseignant de la classe.</li>
 *   <li><b>Consulter</b> : les précédents, plus les responsables régionaux et nationaux, et
 *       <b>un élève ou un parent pour la classe de l'élève concerné</b>.</li>
 * </ul>
 */
@Service("perimetreEmploiDuTemps")
@RequiredArgsConstructor
public class PerimetreEmploiDuTempsService {

    private final PerimetreService perimetreService;
    private final CreneauRepository creneauRepository;
    private final InscriptionRepository inscriptionRepository;
    private final EleveRepository eleveRepository;
    private final LienParentalRepository lienParentalRepository;

    /** Peut modifier ou supprimer ce créneau : comme la classe à laquelle il appartient. */
    @Transactional(readOnly = true)
    public boolean peutGererCreneau(Long creneauId) {
        if (creneauId == null) {
            return false;
        }
        return creneauRepository.findClasseIdById(creneauId)
                .map(perimetreService::peutGererClasse)
                .orElse(false);
    }

    /** Peut consulter ce créneau : comme l'emploi du temps de la classe à laquelle il appartient. */
    @Transactional(readOnly = true)
    public boolean peutConsulterCreneau(Long creneauId) {
        if (creneauId == null) {
            return false;
        }
        return creneauRepository.findClasseIdById(creneauId)
                .map(this::peutConsulterClasse)
                .orElse(false);
    }

    /**
     * Consulter l'emploi du temps d'une classe : la direction et le personnel administratif,
     * les enseignants de la classe, les responsables régionaux et nationaux,
     * et l'élève inscrit dans cette classe ou son parent.
     */
    @Transactional(readOnly = true)
    public boolean peutConsulterClasse(Long classeId) {
        UtilisateurPrincipal p = perimetreService.utilisateurCourant();
        if (p == null || classeId == null) {
            return false;
        }
        // Direction, personnel administratif, enseignants de la classe, périmètres larges.
        if (perimetreService.peutConsulterPresences(classeId)) {
            return true;
        }
        // L'utilisateur est-il l'élève de cette classe ?
        boolean estEleveDeLaClasse = eleveRepository.findByUtilisateurId(p.getId())
                .map(eleve -> inscriptionRepository
                        .existsByClasseIdAndEleveIdAndStatut(classeId, eleve.getId(), "ACCEPTEE"))
                .orElse(false);
        if (estEleveDeLaClasse) {
            return true;
        }
        // Ou le parent d'un élève de cette classe ?
        List<Long> enfants = lienParentalRepository.findEleveIdsByUtilisateurId(p.getId());
        for (Long eleveId : enfants) {
            if (inscriptionRepository.existsByClasseIdAndEleveIdAndStatut(classeId, eleveId, "ACCEPTEE")) {
                return true;
            }
        }
        return false;
    }
}
