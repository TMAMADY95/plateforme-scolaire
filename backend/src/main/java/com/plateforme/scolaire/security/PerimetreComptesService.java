package com.plateforme.scolaire.security;

import com.plateforme.scolaire.domain.AffectationRole;
import com.plateforme.scolaire.domain.Perimetre;
import com.plateforme.scolaire.domain.Role;
import com.plateforme.scolaire.repository.AffectationRoleRepository;
import com.plateforme.scolaire.repository.EleveRepository;
import com.plateforme.scolaire.repository.InscriptionRepository;
import com.plateforme.scolaire.repository.LienParentalRepository;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Qui peut administrer les comptes de qui.
 * <ul>
 *   <li>Le ministère réinitialise les mots de passe des directions (d'établissement et régionales).</li>
 *   <li>La direction d'un établissement réinitialise ceux de son personnel, de ses enseignants et des
 *       parents et élèves inscrits chez elle. Jamais le sien : le changement passe par l'écran dédié.</li>
 *   <li>Une cible qui cumule un rôle hors de ce périmètre est refusée.</li>
 * </ul>
 */
@Service("perimetreComptes")
@RequiredArgsConstructor
public class PerimetreComptesService {

    private static final Set<Role> CIBLES_DU_MINISTERE =
            EnumSet.of(Role.DIRECTION_ETABLISSEMENT, Role.DIRECTION_REGIONALE);

    private final PerimetreService perimetreService;
    private final AffectationRoleRepository affectationRoleRepository;
    private final LienParentalRepository lienParentalRepository;
    private final EleveRepository eleveRepository;
    private final InscriptionRepository inscriptionRepository;

    @Transactional(readOnly = true)
    public boolean estMinistere() {
        UtilisateurPrincipal p = perimetreService.utilisateurCourant();
        return p != null && p.getAffectations().stream().anyMatch(a -> a.role() == Role.MINISTERE);
    }

    @Transactional(readOnly = true)
    public boolean estDirectionDe(Long etablissementId) {
        UtilisateurPrincipal p = perimetreService.utilisateurCourant();
        return p != null && etablissementId != null && p.getAffectations().stream().anyMatch(a ->
                a.role() == Role.DIRECTION_ETABLISSEMENT
                        && a.perimetre() == Perimetre.ETABLISSEMENT
                        && etablissementId.equals(a.etablissementId()));
    }

    @Transactional(readOnly = true)
    public boolean peutListerComptes(Long etablissementId) {
        return estMinistere() || estDirectionDe(etablissementId);
    }

    @Transactional(readOnly = true)
    public boolean peutReinitialiser(Long cibleId) {
        UtilisateurPrincipal p = perimetreService.utilisateurCourant();
        if (p == null || cibleId == null || cibleId.equals(p.getId())) {
            return false;
        }
        List<AffectationRole> cibles = affectationRoleRepository.findByUtilisateurId(cibleId);
        if (cibles.isEmpty()) {
            return false;
        }
        if (estMinistere() && cibles.stream().allMatch(a -> CIBLES_DU_MINISTERE.contains(a.getRole()))) {
            return true;
        }
        Set<Long> etablissementsDiriges = new java.util.HashSet<>();
        p.getAffectations().stream()
                .filter(a -> a.role() == Role.DIRECTION_ETABLISSEMENT && a.perimetre() == Perimetre.ETABLISSEMENT
                        && a.etablissementId() != null)
                .forEach(a -> etablissementsDiriges.add(a.etablissementId()));
        if (etablissementsDiriges.isEmpty()) {
            return false;
        }
        return cibles.stream().allMatch(a -> cibleDeLaDirection(a, cibleId, etablissementsDiriges));
    }

    private boolean cibleDeLaDirection(AffectationRole a, Long cibleId, Set<Long> etablissements) {
        return switch (a.getRole()) {
            case PERSONNEL_ADMIN, ENSEIGNANT ->
                    a.getEtablissement() != null && etablissements.contains(a.getEtablissement().getId());
            case PARENT -> lienParentalRepository.findEleveIdsByUtilisateurId(cibleId).stream()
                    .anyMatch(eleveId -> inscritDans(eleveId, etablissements));
            case ELEVE -> eleveRepository.findByUtilisateurId(cibleId)
                    .map(e -> inscritDans(e.getId(), etablissements)).orElse(false);
            default -> false;
        };
    }

    private boolean inscritDans(Long eleveId, Set<Long> etablissements) {
        return inscriptionRepository.findEtablissementIdsActifsByEleveId(eleveId).stream()
                .anyMatch(etablissements::contains);
    }
}
