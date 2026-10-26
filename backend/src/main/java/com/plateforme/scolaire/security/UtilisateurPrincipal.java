package com.plateforme.scolaire.security;

import com.plateforme.scolaire.domain.Perimetre;
import com.plateforme.scolaire.domain.Role;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/** Utilisateur authentifié, avec ses rôles et leurs périmètres. */
public class UtilisateurPrincipal implements UserDetails {

    /** Un rôle et son périmètre (zoneId ou etablissementId selon le cas). */
    public record AffectationInfo(Role role, Perimetre perimetre, Long zoneId, Long etablissementId) {
    }

    private final Long id;
    private final String email;
    private final String motDePasseHash;
    private final boolean actif;
    private final boolean doitChangerMotDePasse;
    private final Instant verrouilleJusquA;
    private final Instant motDePasseModifieLe;
    private final List<AffectationInfo> affectations;

    public UtilisateurPrincipal(Long id, String email, String motDePasseHash, boolean actif,
                                List<AffectationInfo> affectations) {
        this(id, email, motDePasseHash, actif, false, null, null, affectations);
    }

    public UtilisateurPrincipal(Long id, String email, String motDePasseHash, boolean actif,
                                boolean doitChangerMotDePasse, Instant verrouilleJusquA,
                                Instant motDePasseModifieLe, List<AffectationInfo> affectations) {
        this.id = id;
        this.email = email;
        this.motDePasseHash = motDePasseHash;
        this.actif = actif;
        this.doitChangerMotDePasse = doitChangerMotDePasse;
        this.verrouilleJusquA = verrouilleJusquA;
        this.motDePasseModifieLe = motDePasseModifieLe;
        this.affectations = List.copyOf(affectations);
    }

    public Long getId() {
        return id;
    }

    public List<AffectationInfo> getAffectations() {
        return affectations;
    }

    public boolean isDoitChangerMotDePasse() {
        return doitChangerMotDePasse;
    }

    public Instant getMotDePasseModifieLe() {
        return motDePasseModifieLe;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return affectations.stream()
                .map(a -> new SimpleGrantedAuthority("ROLE_" + a.role().name()))
                .distinct()
                .toList();
    }

    @Override
    public String getPassword() {
        return motDePasseHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return actif;
    }

    /** Un compte verrouillé est refusé avant même la vérification du mot de passe. */
    @Override
    public boolean isAccountNonLocked() {
        return verrouilleJusquA == null || !Instant.now().isBefore(verrouilleJusquA);
    }
}
