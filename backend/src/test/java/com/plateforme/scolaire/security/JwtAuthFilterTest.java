package com.plateforme.scolaire.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.plateforme.scolaire.security.UtilisateurPrincipal.AffectationInfo;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

/** Révocation des jetons, comptes verrouillés et mot de passe initial. */
class JwtAuthFilterTest {

    private static final String SECRET = "test-secret-test-secret-test-secret-123";

    private final JwtService jwtService = new JwtService(SECRET, 5);
    private final CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);
    private final JwtAuthFilter filtre = new JwtAuthFilter(jwtService, userDetailsService);

    @AfterEach
    void nettoyer() {
        SecurityContextHolder.clearContext();
    }

    private UtilisateurPrincipal utilisateur(boolean doitChanger, Instant verrouilleJusquA, Instant modifieLe) {
        UtilisateurPrincipal p = new UtilisateurPrincipal(7L, "u@example.org", "x", true, doitChanger,
                verrouilleJusquA, modifieLe, List.<AffectationInfo>of());
        when(userDetailsService.chargerParId(7L)).thenReturn(Optional.of(p));
        return p;
    }

    private MockHttpServletResponse appeler(String chemin, MockFilterChain chaine) throws Exception {
        MockHttpServletRequest requete = new MockHttpServletRequest("GET", chemin);
        requete.setRequestURI(chemin);
        requete.addHeader("Authorization", "Bearer " + jwtService.generer(7L));
        MockHttpServletResponse reponse = new MockHttpServletResponse();
        filtre.doFilter(requete, reponse, chaine);
        return reponse;
    }

    private MockHttpServletResponse appelerAvecCookie(String methode, String chemin, boolean enTeteApplication, MockFilterChain chaine)
            throws Exception {
        MockHttpServletRequest requete = new MockHttpServletRequest(methode, chemin);
        requete.setRequestURI(chemin);
        requete.setCookies(new Cookie(CookieSession.NOM, jwtService.generer(7L)));
        if (enTeteApplication) {
            requete.addHeader(CookieSession.ENTETE_APPLICATION, "EduManage");
        }
        MockHttpServletResponse reponse = new MockHttpServletResponse();
        filtre.doFilter(requete, reponse, chaine);
        return reponse;
    }

    @Test
    void leCookieAuthentifieUneLectureSansEnTete() throws Exception {
        utilisateur(false, null, null);
        appelerAvecCookie("GET", "/api/etablissements", false, new MockFilterChain());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    @Test
    void uneEcritureParCookieSansEnTeteApplicationEstRefusee() throws Exception {
        utilisateur(false, null, null);
        MockFilterChain chaine = new MockFilterChain();

        MockHttpServletResponse reponse = appelerAvecCookie("POST", "/api/etablissements", false, chaine);

        assertThat(reponse.getStatus()).isEqualTo(403);
        assertThat(reponse.getContentAsString()).contains("EN_TETE_APPLICATION_REQUIS");
        assertThat(chaine.getRequest()).isNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void uneEcritureParCookieAvecEnTeteApplicationEstAcceptee() throws Exception {
        utilisateur(false, null, null);
        MockFilterChain chaine = new MockFilterChain();

        appelerAvecCookie("POST", "/api/etablissements", true, chaine);

        assertThat(chaine.getRequest()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    @Test
    void uneEcritureParJetonNExigePasLEnTeteApplication() throws Exception {
        utilisateur(false, null, null);
        MockHttpServletRequest requete = new MockHttpServletRequest("POST", "/api/etablissements");
        requete.setRequestURI("/api/etablissements");
        requete.addHeader("Authorization", "Bearer " + jwtService.generer(7L));
        MockFilterChain chaine = new MockFilterChain();

        filtre.doFilter(requete, new MockHttpServletResponse(), chaine);

        assertThat(chaine.getRequest()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    @Test
    void leCookieEstIgnoreSurLesRoutesPubliques() throws Exception {
        // Mot de passe initial non changé : le cookie bloquerait la reconnexion s'il était pris en compte.
        utilisateur(true, null, null);
        for (String chemin : List.of("/api/auth/connexion-web", "/api/verification-diplome/ABC")) {
            MockFilterChain chaine = new MockFilterChain();
            appelerAvecCookie("POST", chemin, false, chaine);
            assertThat(chaine.getRequest()).as(chemin).isNotNull();
            assertThat(SecurityContextHolder.getContext().getAuthentication()).as(chemin).isNull();
        }
    }

    @Test
    void unCookieInvalideNAuthentifiePersonne() throws Exception {
        MockHttpServletRequest requete = new MockHttpServletRequest("GET", "/api/me");
        requete.setRequestURI("/api/me");
        requete.setCookies(new Cookie(CookieSession.NOM, "pas-un-jeton"));
        MockFilterChain chaine = new MockFilterChain();

        filtre.doFilter(requete, new MockHttpServletResponse(), chaine);

        assertThat(chaine.getRequest()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void unJetonValideAuthentifieLUtilisateur() throws Exception {
        utilisateur(false, null, null);
        appeler("/api/etablissements", new MockFilterChain());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    @Test
    void unJetonEmisAvantLeChangementDeMotDePasseEstRevoque() throws Exception {
        utilisateur(false, null, Instant.now().plusSeconds(30));
        appeler("/api/etablissements", new MockFilterChain());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void unJetonEmisApresLeChangementDeMotDePasseResteValide() throws Exception {
        utilisateur(false, null, Instant.now().minusSeconds(30));
        appeler("/api/etablissements", new MockFilterChain());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    @Test
    void unCompteVerrouilleEstRefuseMemeAvecUnJetonValide() throws Exception {
        utilisateur(false, Instant.now().plusSeconds(600), null);
        appeler("/api/etablissements", new MockFilterChain());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void unMotDePasseInitialBloqueLesEndpointsMetier() throws Exception {
        utilisateur(true, null, null);
        MockFilterChain chaine = new MockFilterChain();

        MockHttpServletResponse reponse = appeler("/api/etablissements", chaine);

        assertThat(reponse.getStatus()).isEqualTo(403);
        assertThat(reponse.getContentAsString()).contains("CHANGEMENT_MOT_DE_PASSE_REQUIS");
        assertThat(chaine.getRequest()).isNull();
    }

    @Test
    void unMotDePasseInitialAutoriseLeChangementDeMotDePasse() throws Exception {
        utilisateur(true, null, null);
        MockFilterChain chaine = new MockFilterChain();

        appeler("/api/me/mot-de-passe", chaine);

        assertThat(chaine.getRequest()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }
}
