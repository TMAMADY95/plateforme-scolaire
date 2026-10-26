package com.plateforme.scolaire.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "test-secret-test-secret-test-secret-123";

    @Test
    void unJetonValideRetourneLIdentifiantEtLaDateDEmission() {
        JwtService service = new JwtService(SECRET, 5);
        Instant avant = Instant.now().minusSeconds(2);
        String token = service.generer(42L);

        var info = service.analyser(token);

        assertThat(info).isPresent();
        assertThat(info.get().idUtilisateur()).isEqualTo(42L);
        assertThat(info.get().emisLe()).isAfter(avant).isBeforeOrEqualTo(Instant.now().plus(Duration.ofSeconds(2)));
    }

    @Test
    void laDateDEmissionEstPreciseALaMillisecondePourLaRevocation() {
        JwtService service = new JwtService(SECRET, 5);
        Instant avant = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        String token = service.generer(42L);
        Instant apres = Instant.now();

        Instant emisLe = service.analyser(token).orElseThrow().emisLe();

        assertThat(emisLe).isAfterOrEqualTo(avant).isBeforeOrEqualTo(apres);
    }

    @Test
    void unJetonAltereEstRefuse() {
        JwtService service = new JwtService(SECRET, 5);
        String token = service.generer(42L);
        assertThat(service.analyser(token + "x")).isEmpty();
    }

    @Test
    void unJetonSigneAvecUneAutreCleEstRefuse() {
        String token = new JwtService(SECRET, 5).generer(42L);
        JwtService autre = new JwtService("autre-secret-autre-secret-autre-secret-9", 5);
        assertThat(autre.analyser(token)).isEmpty();
    }

    @Test
    void unJetonExpireEstRefuse() {
        JwtService service = new JwtService(SECRET, -1);
        assertThat(service.analyser(service.generer(42L))).isEmpty();
    }

    @Test
    void unTexteQuelconqueEstRefuse() {
        assertThat(new JwtService(SECRET, 5).analyser("pas-un-jeton")).isEmpty();
    }

    @Test
    void unSecretTropCourtEstRefuse() {
        assertThatThrownBy(() -> new JwtService("court", 5)).isInstanceOf(IllegalStateException.class);
    }
}
