package com.plateforme.scolaire.security;

import com.plateforme.scolaire.repository.CreneauRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autorisations des emplois du temps, utilisables dans les annotations :
 * <pre>@PreAuthorize("@perimetreEmploiDuTemps.peutGererCreneau(#id)")</pre>
 *
 * <p>Même découpage que {@link PerimetreNotesService} : les règles propres aux emplois du temps
 * ne chargent pas {@link PerimetreService} d'une dépendance supplémentaire.</p>
 *
 * <ul>
 *   <li><b>Écrire</b> : mêmes règles que la classe (direction, personnel administratif, ou enseignant de la classe).</li>
 *   <li><b>Lire</b> : mêmes règles que les présences (direction, enseignants de la classe, responsables
 *       régionaux et nationaux).</li>
 * </ul>
 */
@Service("perimetreEmploiDuTemps")
@RequiredArgsConstructor
public class PerimetreEmploiDuTempsService {

    private final PerimetreService perimetreService;
    private final CreneauRepository creneauRepository;

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

    /** Peut consulter ce créneau : comme les présences de la classe à laquelle il appartient. */
    @Transactional(readOnly = true)
    public boolean peutConsulterCreneau(Long creneauId) {
        if (creneauId == null) {
            return false;
        }
        return creneauRepository.findClasseIdById(creneauId)
                .map(perimetreService::peutConsulterPresences)
                .orElse(false);
    }
}
