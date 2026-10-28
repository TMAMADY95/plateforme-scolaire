package com.plateforme.scolaire.security;

import com.plateforme.scolaire.domain.AffectationRole;
import com.plateforme.scolaire.domain.Utilisateur;
import com.plateforme.scolaire.repository.AffectationRoleRepository;
import com.plateforme.scolaire.repository.UtilisateurRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UtilisateurRepository utilisateurRepository;
    private final AffectationRoleRepository affectationRoleRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return utilisateurRepository.findByEmailIgnoreCase(email)
                .map(this::construire)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable"));
    }

    @Transactional(readOnly = true)
    public Optional<UtilisateurPrincipal> chargerParId(Long id) {
        return utilisateurRepository.findById(id).map(this::construire);
    }

    /**
     * Un rôle rattaché à un établissement SUSPENDU ou FERME ne donne plus aucun droit. Si c'est
     * tout ce que possède le compte, il est désactivé : connexion refusée (même réponse que pour
     * un mauvais mot de passe) et jetons déjà émis rejetés dès la requête suivante, puisque le
     * filtre JWT recharge l'utilisateur à chaque appel. Réactiver l'établissement rend l'accès.
     */
    private UtilisateurPrincipal construire(Utilisateur u) {
        List<AffectationRole> toutes = affectationRoleRepository.findByUtilisateurId(u.getId());
        List<UtilisateurPrincipal.AffectationInfo> affectations = toutes.stream()
                .filter(CustomUserDetailsService::etablissementActif)
                .map(this::versInfo)
                .toList();
        boolean toutesSuspendues = !toutes.isEmpty() && affectations.isEmpty();
        return new UtilisateurPrincipal(u.getId(), u.getEmail(), u.getMotDePasseHash(),
                u.isActif() && !toutesSuspendues,
                u.isDoitChangerMotDePasse(), u.getVerrouilleJusquA(), u.getMotDePasseModifieLe(), affectations);
    }

    private static boolean etablissementActif(AffectationRole a) {
        return a.getEtablissement() == null || "ACTIF".equals(a.getEtablissement().getStatut());
    }

    private UtilisateurPrincipal.AffectationInfo versInfo(AffectationRole a) {
        return new UtilisateurPrincipal.AffectationInfo(
                a.getRole(),
                a.getPerimetre(),
                a.getZone() != null ? a.getZone().getId() : null,
                a.getEtablissement() != null ? a.getEtablissement().getId() : null);
    }
}
