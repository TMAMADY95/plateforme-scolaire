package com.plateforme.scolaire.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.plateforme.scolaire.domain.AffectationRole;
import com.plateforme.scolaire.domain.Etablissement;
import com.plateforme.scolaire.domain.Perimetre;
import com.plateforme.scolaire.domain.Role;
import com.plateforme.scolaire.domain.Utilisateur;
import com.plateforme.scolaire.repository.AffectationRoleRepository;
import com.plateforme.scolaire.repository.UtilisateurRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Un établissement suspendu ou fermé ne laisse plus entrer ceux qui n'ont de rôle que chez lui. */
class CustomUserDetailsServiceTest {

    private final UtilisateurRepository utilisateurRepository = mock(UtilisateurRepository.class);
    private final AffectationRoleRepository affectationRepository = mock(AffectationRoleRepository.class);
    private final CustomUserDetailsService service =
            new CustomUserDetailsService(utilisateurRepository, affectationRepository);

    private Utilisateur utilisateur() {
        Utilisateur u = new Utilisateur();
        u.setId(1L);
        u.setEmail("u@example.org");
        u.setMotDePasseHash("x");
        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(u));
        return u;
    }

    private static AffectationRole role(Role role, Perimetre perimetre, String statutEtablissement) {
        AffectationRole a = new AffectationRole();
        a.setRole(role);
        a.setPerimetre(perimetre);
        if (statutEtablissement != null) {
            Etablissement e = new Etablissement();
            e.setId(5L);
            e.setStatut(statutEtablissement);
            a.setEtablissement(e);
        }
        return a;
    }

    @Test
    void lesRolesDUnEtablissementActifRestentValides() {
        utilisateur();
        when(affectationRepository.findByUtilisateurId(1L)).thenReturn(
                List.of(role(Role.DIRECTION_ETABLISSEMENT, Perimetre.ETABLISSEMENT, "ACTIF")));

        UtilisateurPrincipal p = service.chargerParId(1L).orElseThrow();

        assertThat(p.isEnabled()).isTrue();
        assertThat(p.getAffectations()).hasSize(1);
    }

    @Test
    void uneDirectionDUnEtablissementSuspenduEstDesactivee() {
        utilisateur();
        when(affectationRepository.findByUtilisateurId(1L)).thenReturn(
                List.of(role(Role.DIRECTION_ETABLISSEMENT, Perimetre.ETABLISSEMENT, "SUSPENDU")));

        UtilisateurPrincipal p = service.chargerParId(1L).orElseThrow();

        assertThat(p.isEnabled()).isFalse();
        assertThat(p.getAffectations()).isEmpty();
    }

    @Test
    void unCompteAvecAussiUnAutreRoleResteActifMaisPerdLeRoleSuspendu() {
        utilisateur();
        when(affectationRepository.findByUtilisateurId(1L)).thenReturn(List.of(
                role(Role.ENSEIGNANT, Perimetre.ETABLISSEMENT, "FERME"),
                role(Role.PARENT, Perimetre.PERSONNEL, null)));

        UtilisateurPrincipal p = service.chargerParId(1L).orElseThrow();

        assertThat(p.isEnabled()).isTrue();
        assertThat(p.getAffectations()).extracting(UtilisateurPrincipal.AffectationInfo::role)
                .containsExactly(Role.PARENT);
    }

    @Test
    void leMinistereNEstJamaisAffecteParLesStatutsDEtablissement() {
        utilisateur();
        when(affectationRepository.findByUtilisateurId(1L)).thenReturn(
                List.of(role(Role.MINISTERE, Perimetre.NATIONAL, null)));

        assertThat(service.chargerParId(1L).orElseThrow().isEnabled()).isTrue();
    }
}
