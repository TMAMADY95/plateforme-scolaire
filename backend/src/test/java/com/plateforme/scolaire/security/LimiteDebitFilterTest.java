package com.plateforme.scolaire.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class LimiteDebitFilterTest {

    private static final String LOGIN = "/api/auth/login";

    /** Horloge réglable. */
    private static final class Horloge extends Clock {
        Instant maintenant = Instant.parse("2026-10-29T10:00:00Z");

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return maintenant;
        }
    }

    private final Horloge horloge = new Horloge();
    private final LimiteDebitFilter filtre = new LimiteDebitFilter(
            Map.of(LOGIN, new LimiteDebitFilter.Regle(2, Duration.ofMinutes(1))), horloge);

    private MockHttpServletResponse appeler(String methode, String chemin, String ip) throws Exception {
        MockHttpServletRequest requete = new MockHttpServletRequest(methode, chemin);
        requete.setRemoteAddr(ip);
        MockHttpServletResponse reponse = new MockHttpServletResponse();
        filtre.doFilter(requete, reponse, new MockFilterChain());
        return reponse;
    }

    @Test
    void auDelaDeLaLimiteLaReponseEst429AvecRetryAfter() throws Exception {
        assertThat(appeler("POST", LOGIN, "1.1.1.1").getStatus()).isEqualTo(200);
        assertThat(appeler("POST", LOGIN, "1.1.1.1").getStatus()).isEqualTo(200);

        MockHttpServletResponse refusee = appeler("POST", LOGIN, "1.1.1.1");

        assertThat(refusee.getStatus()).isEqualTo(429);
        assertThat(refusee.getHeader("Retry-After")).isEqualTo("60");
        assertThat(refusee.getContentAsString()).contains("Trop de tentatives");
    }

    @Test
    void chaqueAdresseALeSienCompteurEtLaFenetreSeRenouvelle() throws Exception {
        appeler("POST", LOGIN, "1.1.1.1");
        appeler("POST", LOGIN, "1.1.1.1");
        assertThat(appeler("POST", LOGIN, "2.2.2.2").getStatus()).isEqualTo(200);

        horloge.maintenant = horloge.maintenant.plusSeconds(61);

        assertThat(appeler("POST", LOGIN, "1.1.1.1").getStatus()).isEqualTo(200);
    }

    @Test
    void lesAutresCheminsEtMethodesNeSontPasLimites() throws Exception {
        for (int i = 0; i < 10; i++) {
            assertThat(appeler("GET", LOGIN, "1.1.1.1").getStatus()).isEqualTo(200);
            assertThat(appeler("POST", "/api/me", "1.1.1.1").getStatus()).isEqualTo(200);
        }
    }
}
