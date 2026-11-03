package com.plateforme.scolaire.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Matrice des droits sur les examens, contre un vrai serveur et PostgreSQL (Testcontainers) : le ministère fixe et proclame,
 * la DGECS gère la technique, une direction d'établissement ne voit pas la supervision. Les identifiants utilisés n'existent pas :
 * seul le contrôle d'accès est vérifié (403 refusé, tout autre code accepté). Ignoré si Docker n'est pas disponible.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
@EnabledIf(value = "dockerDisponible", disabledReason = "Docker n'est pas disponible : tests d'intégration ignorés")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DroitsExamensIntegrationTest {

    private static final String MDP = "Demo!2026";
    private static final long INCONNU = 999_999L;

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

    private String ministere;
    private String dgecs;
    private String direction;

    private int appeler(String methode, String chemin, String jeton, String corps) throws Exception {
        HttpRequest.Builder requete = HttpRequest.newBuilder(URI.create("http://localhost:" + port + chemin))
                .header("Content-Type", "application/json");
        if (jeton != null) {
            requete.header("Authorization", "Bearer " + jeton);
        }
        requete.method(methode, corps == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(corps));
        return http.send(requete.build(), HttpResponse.BodyHandlers.ofString()).statusCode();
    }

    private String connecter(String email) throws Exception {
        HttpRequest requete = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"email\":\"%s\",\"motDePasse\":\"%s\"}".formatted(email, MDP))).build();
        HttpResponse<String> reponse = http.send(requete, HttpResponse.BodyHandlers.ofString());
        assertThat(reponse.statusCode()).as("connexion de %s : %s", email, reponse.body()).isEqualTo(200);
        return com.jayway.jsonpath.JsonPath.read(reponse.body(), "$.token");
    }

    @BeforeAll
    void connexions() throws Exception {
        ministere = connecter("ministere@example.org");
        dgecs = connecter("dgecs@example.org");
        direction = connecter("direction1@example.org");
    }

    private void refuse(String methode, String chemin, String jeton, String corps) throws Exception {
        assertThat(appeler(methode, chemin, jeton, corps)).as("%s %s doit être refusé", methode, chemin).isEqualTo(403);
    }

    private void accepte(String methode, String chemin, String jeton, String corps) throws Exception {
        assertThat(appeler(methode, chemin, jeton, corps)).as("%s %s ne doit pas être refusé", methode, chemin).isNotIn(401, 403);
    }

    @Test
    void sansJetonLAccesEstRefuse() throws Exception {
        assertThat(appeler("GET", "/api/examens", null, null)).isEqualTo(401);
    }

    @Test
    void consultationOuverteAuMinistereEtALaDgecsPasAUneDirectionDEtablissement() throws Exception {
        accepte("GET", "/api/examens?anneeScolaireId=1", ministere, null);
        accepte("GET", "/api/examens?anneeScolaireId=1", dgecs, null);
        refuse("GET", "/api/examens?anneeScolaireId=1", direction, null);
        accepte("GET", "/api/examens/" + INCONNU + "/suivi-listes", ministere, null);
        accepte("GET", "/api/examens/" + INCONNU + "/suivi-listes", dgecs, null);
        refuse("GET", "/api/examens/" + INCONNU + "/suivi-listes", direction, null);
    }

    @Test
    void laDgecsOrganiseLesExamensEtLeMinistereSeulProclame() throws Exception {
        String examen = "{\"libelle\":\"Examen test\",\"anneeScolaireId\":1,\"cycleId\":1}";
        accepte("POST", "/api/examens", dgecs, examen);
        refuse("POST", "/api/examens", ministere, examen);
        refuse("POST", "/api/examens", direction, examen);
        refuse("PUT", "/api/centres-examen/criteres", ministere,
                "{\"secteurAdmis\":\"TOUS\",\"autorisationRequise\":false,\"sallesMinimum\":0,\"capaciteMinimum\":0}");

        accepte("POST", "/api/examens/" + INCONNU + "/proclamation", ministere, null);
        refuse("POST", "/api/examens/" + INCONNU + "/proclamation", dgecs, null);
        refuse("POST", "/api/examens/" + INCONNU + "/proclamation", direction, null);

        String calendrier = "{}";
        accepte("PUT", "/api/examens/" + INCONNU + "/calendrier-previsionnel", dgecs, calendrier);
        refuse("PUT", "/api/examens/" + INCONNU + "/calendrier-previsionnel", ministere, calendrier);
        refuse("PUT", "/api/examens/" + INCONNU + "/periode", ministere, calendrier);
        refuse("PUT", "/api/examens/" + INCONNU + "/calendrier-previsionnel", direction, calendrier);
    }

    @Test
    void laDeliberationEstTechniqueDoncPourLaDgecsEtPasLeMinistere() throws Exception {
        accepte("POST", "/api/examens/" + INCONNU + "/deliberation", dgecs, null);
        refuse("POST", "/api/examens/" + INCONNU + "/deliberation", ministere, null);
        refuse("POST", "/api/examens/" + INCONNU + "/deliberation", direction, null);
    }

    @Test
    void lesResultatsDUnEleveInconnuSontRefuses() throws Exception {
        refuse("GET", "/api/eleves/" + INCONNU + "/resultats-examens", direction, null);
    }
}
