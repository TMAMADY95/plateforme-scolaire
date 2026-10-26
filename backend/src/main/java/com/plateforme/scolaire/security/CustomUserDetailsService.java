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

    private UtilisateurPrincipal construire(Utilisateur u) {
        List<UtilisateurPrincipal.AffectationInfo> affectations =
                affectationRoleRepository.findByUtilisateurId(u.getId()).stream()
                        .map(this::versInfo)
                        .toList();
        return new UtilisateurPrincipal(u.getId(), u.getEmail(), u.getMotDePasseHash(), u.isActif(),
                u.isDoitChangerMotDePasse(), u.getVerrouilleJusquA(), u.getMotDePasseModifieLe(), affectations);
    }

    private UtilisateurPrincipal.AffectationInfo versInfo(AffectationRole a) {
        return new UtilisateurPrincipal.AffectationInfo(
                a.getRole(),
                a.getPerimetre(),
                a.getZone() != null ? a.getZone().getId() : null,
                a.getEtablissement() != null ? a.getEtablissement().getId() : null);
    }
}
