package com.plateforme.scolaire.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Session du site web par cookie HttpOnly, contre un vrai serveur et PostgreSQL (Testcontainers) ; la connexion mobile par jeton
 * ne doit pas changer. Ignoré si Docker n'est pas disponible.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
@EnabledIf(value = "dockerDisponible", disabledReason = "Docker n'est pas disponible : tests d'intégration ignorés")
class CookieSessionIntegrationTest {

    private static final String IDENTIFIANTS = "{\"email\":\"ministere@example.org\",\"motDePasse\":\"Demo!2026\"}";
    private static final String CHANGEMENT_REFUSE = "{\"motDePasseActuel\":\"faux-mot-de-passe\",\"nouveauMotDePasse\":\"Nouveau!2026-abc\"}";

    static boolean dockerDisponible() {
        return DockerClientFactory.instance().isDockerAvailable();
    }

    @DynamicPropertySource
    static void baseDeDonnees(DynamicPropertyRegistry registry) {
        PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");
        postgres.start();
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    private final HttpClient http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();

    @Value("${local.server.port}")
    private int port;

    private HttpRequest.Builder requete(String chemin) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + port + chemin)).header("Content-Type", "application/json");
    }

    private HttpResponse<String> envoyer(HttpRequest.Builder requete) throws Exception {
        return http.send(requete.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> connexionWeb() throws Exception {
        return envoyer(requete("/api/auth/connexion-web").POST(HttpRequest.BodyPublishers.ofString(IDENTIFIANTS)));
    }

    /** Partie "nom=valeur" du cookie, telle que le navigateur la renverrait. */
    private String cookieDe(HttpResponse<String> reponse) {
        return reponse.headers().firstValue("Set-Cookie").orElseThrow().split(";")[0];
    }

    @Test
    void laConnexionWebPoseUnCookieProtegeEtNeRenvoieAucunJeton() throws Exception {
        HttpResponse<String> reponse = connexionWeb();

        assertThat(reponse.statusCode()).isEqualTo(204);
        assertThat(reponse.body()).isEmpty();
        assertThat(reponse.headers().firstValue("Set-Cookie").orElseThrow())
                .startsWith("session_scolaire=")
                .contains("HttpOnly", "SameSite=Strict", "Path=/api")
                .doesNotContain("Max-Age", "Expires");
    }

    @Test
    void uneConnexionWebRefuseeNePoseAucunCookie() throws Exception {
        HttpResponse<String> reponse = envoyer(requete("/api/auth/connexion-web").POST(HttpRequest.BodyPublishers.ofString(
                "{\"email\":\"ministere@example.org\",\"motDePasse\":\"faux\"}")));

        assertThat(reponse.statusCode()).isEqualTo(401);
        assertThat(reponse.headers().firstValue("Set-Cookie")).isEmpty();
    }

    @Test
    void leCookieOuvreLaSession() throws Exception {
        String cookie = cookieDe(connexionWeb());

        assertThat(envoyer(requete("/api/me")).statusCode()).isEqualTo(401);
        HttpResponse<String> moi = envoyer(requete("/api/me").header("Cookie", cookie));
        assertThat(moi.statusCode()).isEqualTo(200);
        assertThat(moi.body()).contains("ministere@example.org");
    }

    @Test
    void uneEcritureParCookieExigeLEnTeteDeLApplication() throws Exception {
        String cookie = cookieDe(connexionWeb());

        HttpResponse<String> sansEnTete = envoyer(requete("/api/me/mot-de-passe").header("Cookie", cookie)
                .POST(HttpRequest.BodyPublishers.ofString(CHANGEMENT_REFUSE)));
        assertThat(sansEnTete.statusCode()).isEqualTo(403);
        assertThat(sansEnTete.body()).contains("EN_TETE_APPLICATION_REQUIS");

        // Le contrôle est franchi : le serveur refuse alors le faux mot de passe actuel, pour une autre raison.
        HttpResponse<String> avecEnTete = envoyer(requete("/api/me/mot-de-passe").header("Cookie", cookie)
                .header("X-Requested-With", "EduManage").POST(HttpRequest.BodyPublishers.ofString(CHANGEMENT_REFUSE)));
        assertThat(avecEnTete.statusCode()).isNotIn(401, 403);
        assertThat(avecEnTete.body()).doesNotContain("EN_TETE_APPLICATION_REQUIS");
    }

    @Test
    void laDeconnexionEffaceLeCookieEtExigeLEnTete() throws Exception {
        assertThat(envoyer(requete("/api/auth/deconnexion").POST(HttpRequest.BodyPublishers.noBody())).statusCode()).isEqualTo(400);

        HttpResponse<String> reponse = envoyer(requete("/api/auth/deconnexion").header("X-Requested-With", "EduManage")
                .POST(HttpRequest.BodyPublishers.noBody()));

        assertThat(reponse.statusCode()).isEqualTo(204);
        assertThat(reponse.headers().firstValue("Set-Cookie").orElseThrow())
                .startsWith("session_scolaire=;").contains("Max-Age=0", "HttpOnly", "Path=/api");
    }

    @Test
    void laConnexionMobileParJetonNeChangePas() throws Exception {
        HttpResponse<String> reponse = envoyer(requete("/api/auth/login").POST(HttpRequest.BodyPublishers.ofString(IDENTIFIANTS)));

        assertThat(reponse.statusCode()).isEqualTo(200);
        assertThat(reponse.headers().firstValue("Set-Cookie")).isEmpty();
        String jeton = com.jayway.jsonpath.JsonPath.read(reponse.body(), "$.token");
        assertThat(envoyer(requete("/api/me").header("Authorization", "Bearer " + jeton)).statusCode()).isEqualTo(200);
        // Une écriture par jeton n'a pas besoin de l'en-tête de l'application.
        HttpResponse<String> ecriture = envoyer(requete("/api/me/mot-de-passe").header("Authorization", "Bearer " + jeton)
                .POST(HttpRequest.BodyPublishers.ofString(CHANGEMENT_REFUSE)));
        assertThat(ecriture.body()).doesNotContain("EN_TETE_APPLICATION_REQUIS");
    }

    @Test
    void seulesLesOriginesAutoriseesPeuventJoindreLeCookie() throws Exception {
        HttpResponse<String> autorisee = envoyer(preflight("http://localhost:5173"));
        assertThat(autorisee.statusCode()).isEqualTo(200);
        assertThat(autorisee.headers().firstValue("Access-Control-Allow-Origin")).contains("http://localhost:5173");
        assertThat(autorisee.headers().firstValue("Access-Control-Allow-Credentials")).contains("true");

        HttpResponse<String> tierce = envoyer(preflight("https://site-tiers.example"));
        assertThat(tierce.statusCode()).isEqualTo(403);
        assertThat(tierce.headers().firstValue("Access-Control-Allow-Origin")).isEmpty();
    }

    private HttpRequest.Builder preflight(String origine) {
        return requete("/api/me/mot-de-passe").header("Origin", origine)
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "x-requested-with,content-type")
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody());
    }
}
