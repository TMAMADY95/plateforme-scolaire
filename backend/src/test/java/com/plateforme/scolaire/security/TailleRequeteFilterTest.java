package com.plateforme.scolaire.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class TailleRequeteFilterTest {

    private final TailleRequeteFilter filtre = new TailleRequeteFilter(100);

    private MockHttpServletResponse appeler(byte[] corps) throws Exception {
        MockHttpServletRequest requete = new MockHttpServletRequest("POST", "/api/auth/login");
        requete.setContent(corps);
        MockHttpServletResponse reponse = new MockHttpServletResponse();
        MockFilterChain chaine = new MockFilterChain();
        filtre.doFilter(requete, reponse, chaine);
        if (reponse.getStatus() == 200) {
            assertThat(chaine.getRequest()).isNotNull();
        } else {
            assertThat(chaine.getRequest()).isNull();
        }
        return reponse;
    }

    @Test
    void uneRequetePetiteEstLaissee() throws Exception {
        assertThat(appeler(new byte[100]).getStatus()).isEqualTo(200);
    }

    @Test
    void uneRequeteTropGrosseEstRefusee() throws Exception {
        assertThat(appeler(new byte[101]).getStatus()).isEqualTo(413);
    }
}
