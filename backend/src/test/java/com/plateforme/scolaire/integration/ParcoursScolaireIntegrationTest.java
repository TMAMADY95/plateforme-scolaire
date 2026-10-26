package com.plateforme.scolaire.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Parcours complet contre un vrai PostgreSQL (lancé par Testcontainers) et un vrai serveur HTTP :
 * référentiels, cloisonnement, classes, inscriptions, comptes, appel, notes, moyennes, parents, verrouillage.
 *
 * Les étapes s'exécutent dans l'ordre et se partagent leurs données : si l'une échoue, les suivantes échouent aussi.
 * Ignoré automatiquement si Docker n'est pas disponible. Le profil "dev" fournit les comptes de démonstration.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
@EnabledIf(value = "dockerDisponible", disabledReason = "Docker n'est pas disponible : tests d'intégration ignorés")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ParcoursScolaireIntegrationTest {

    private static final String MDP_DEMO = "Demo!2026";
    private static final String MDP_INITIAL = "MotDePasseInitial1!";
    private static final String MDP_NOUVEAU = "NouveauMotDePasse2026!";
    private static final String EMAIL_ENSEIGNANT = "prof-it@example.org";
    private static final String EMAIL_PARENT = "parent-it@example.org";

    static boolean dockerDisponible() {
        return DockerClientFactory.instance().isDockerAvailable();
    }

    /** Appelée seulement quand le contexte Spring est créé, donc jamais si Docker est absent. */
    @DynamicPropertySource
    static void baseDeDonnees(DynamicPropertyRegistry registry) {
        PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");
        postgres.start(); // arrêté automatiquement à la fin de la JVM (Ryuk)
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    // ---------- Client HTTP ----------

    private record Reponse(String description, int statut, String corps) {

        Reponse attendre(int attendu) {
            assertThat(statut).as("%s -> %s", description, corps).isEqualTo(attendu);
            return this;
        }

        Object lire(String chemin) {
            return JsonPath.read(corps, chemin);
        }

        long id(String chemin) {
            return ((Number) lire(chemin)).longValue();
        }

        String texte(String chemin) {
            return (String) lire(chemin);
        }

        double nombre(String chemin) {
            return ((Number) lire(chemin)).doubleValue();
        }
    }

    private final HttpClient http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private JdbcTemplate jdbc;

    private Reponse appeler(String methode, String chemin, String jeton, String corps) throws Exception {
        HttpRequest.Builder requete = HttpRequest.newBuilder(URI.create("http://localhost:" + port + chemin))
                .header("Content-Type", "application/json");
        if (jeton != null) {
            requete.header("Authorization", "Bearer " + jeton);
        }
        requete.method(methode, corps == null
                ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(corps));
        HttpResponse<String> reponse = http.send(requete.build(), HttpResponse.BodyHandlers.ofString());
        return new Reponse(methode + " " + chemin, reponse.statusCode(), reponse.body());
    }

    private Reponse get(String chemin, String jeton) throws Exception {
        return appeler("GET", chemin, jeton, null);
    }

    private Reponse post(String chemin, String jeton, String corps) throws Exception {
        return appeler("POST", chemin, jeton, corps);
    }

    private Reponse put(String chemin, String jeton, String corps) throws Exception {
        return appeler("PUT", chemin, jeton, corps);
    }

    private Reponse connecter(String email, String motDePasse) throws Exception {
        return post("/api/auth/login", null, """
                {"email":"%s","motDePasse":"%s"}""".formatted(email, motDePasse));
    }

    private String jeton(String email, String motDePasse) throws Exception {
        return connecter(email, motDePasse).attendre(200).texte("$.token");
    }

    /** Première connexion d'un compte créé avec un mot de passe initial : blocage, changement, révocation. */
    private String premiereConnexion(String email) throws Exception {
        Reponse connexion = connecter(email, MDP_INITIAL).attendre(200);
        assertThat(connexion.lire("$.changementMotDePasseRequis")).isEqualTo(true);
        String ancienJeton = connexion.texte("$.token");

        get("/api/etablissements", ancienJeton).attendre(403);
        get("/api/me", ancienJeton).attendre(200);
        post("/api/me/mot-de-passe", ancienJeton, """
                {"motDePasseActuel":"%s","nouveauMotDePasse":"%s"}""".formatted(MDP_INITIAL, MDP_NOUVEAU))
                .attendre(204);

        get("/api/me", ancienJeton).attendre(401); // l'ancien jeton est révoqué
        Reponse nouvelle = connecter(email, MDP_NOUVEAU).attendre(200);
        assertThat(nouvelle.lire("$.changementMotDePasseRequis")).isEqualTo(false);
        return nouvelle.texte("$.token");
    }

    private static long idEtablissement(Reponse liste, String code) {
        List<?> ids = (List<?>) liste.lire("$[?(@.codeOfficiel=='" + code + "')].id");
        return ((Number) ids.get(0)).longValue();
    }

    // ---------- État partagé entre les étapes ----------

    private final LocalDate aujourdhui = LocalDate.now();
    private final String suffixe = UUID.randomUUID().toString().substring(0, 8);

    private String jetonMinistere;
    private String jetonDirection;
    private String jetonEnseignant;
    private String jetonParent;
    private long ecole1;
    private long ecole2;
    private long anneeId;
    private long periodeId;
    private long niveauId;
    private long matiereId;
    private long classeId;
    private long inscriptionId;
    private long eleveId;
    private long eleve2Id;
    private long enseignantId;
    private long evaluationId;

    // ---------- Parcours ----------

    @Test
    @Order(1)
    void leMinisterePrepareLesReferentiels() throws Exception {
        jetonMinistere = jeton("ministere@example.org", MDP_DEMO);

        Reponse etablissements = get("/api/etablissements", jetonMinistere).attendre(200);
        ecole1 = idEtablissement(etablissements, "ECOLE-DEMO-1");
        ecole2 = idEtablissement(etablissements, "ECOLE-DEMO-2");

        anneeId = post("/api/annees-scolaires", jetonMinistere, """
                {"libelle":"IT-%s","dateDebut":"%s","dateFin":"%s"}"""
                .formatted(suffixe, aujourdhui.minusDays(120), aujourdhui.plusDays(200))).attendre(201).id("$.id");
        periodeId = post("/api/annees-scolaires/" + anneeId + "/periodes", jetonMinistere, """
                {"libelle":"Période IT","ordre":1,"dateDebut":"%s","dateFin":"%s"}"""
                .formatted(aujourdhui.minusDays(100), aujourdhui.plusDays(100))).attendre(201).id("$.id");
        niveauId = post("/api/niveaux", jetonMinistere, """
                {"code":"IT-%s","libelle":"Niveau IT","ordre":90}""".formatted(suffixe)).attendre(201).id("$.id");
        matiereId = post("/api/matieres", jetonMinistere, """
                {"code":"IT-%s","libelle":"Matière IT"}""".formatted(suffixe)).attendre(201).id("$.id");
    }

    @Test
    @Order(2)
    void chaqueUtilisateurNeVoitQueSonPerimetre() throws Exception {
        jetonDirection = jeton("direction1@example.org", MDP_DEMO);

        Reponse liste = get("/api/etablissements", jetonDirection).attendre(200);
        assertThat(liste.lire("$.length()")).isEqualTo(1);
        assertThat(liste.texte("$[0].codeOfficiel")).isEqualTo("ECOLE-DEMO-1");

        get("/api/etablissements/" + ecole2, jetonDirection).attendre(403);
        post("/api/niveaux", jetonDirection, """
                {"code":"X","libelle":"X","ordre":1}""").attendre(403);
        String classe = """
                {"anneeScolaireId":%d,"niveauId":%d,"nom":"B","capacite":10}""".formatted(anneeId, niveauId);
        post("/api/etablissements/" + ecole2 + "/classes", jetonDirection, classe).attendre(403);
        post("/api/etablissements/" + ecole1 + "/classes", jetonMinistere, classe).attendre(403);
    }

    @Test
    @Order(3)
    void laDirectionCreeUneClasse() throws Exception {
        String classe = """
                {"anneeScolaireId":%d,"niveauId":%d,"nom":"A","capacite":1}""".formatted(anneeId, niveauId);
        classeId = post("/api/etablissements/" + ecole1 + "/classes", jetonDirection, classe)
                .attendre(201).id("$.id");
        post("/api/etablissements/" + ecole1 + "/classes", jetonDirection, classe).attendre(409);
    }

    @Test
    @Order(4)
    void uneInscriptionCreeLEleveEtLesDoublonsSontRefuses() throws Exception {
        String demande = """
                {"anneeScolaireId":%d,"nouvelEleve":{"nom":"Dupont","prenom":"Jean","dateNaissance":"2015-05-04","sexe":"M"}}"""
                .formatted(anneeId);
        Reponse inscription = post("/api/etablissements/" + ecole1 + "/inscriptions", jetonDirection, demande)
                .attendre(201);
        inscriptionId = inscription.id("$.id");
        eleveId = inscription.id("$.eleve.id");
        assertThat(inscription.texte("$.statut")).isEqualTo("EN_ATTENTE");
        String matricule = inscription.texte("$.eleve.matricule");
        assertThat(matricule).startsWith("ELV-");

        // Même identité : refusé. Même élève par son matricule : refusé aussi (déjà inscrit cette année).
        post("/api/etablissements/" + ecole1 + "/inscriptions", jetonDirection, demande).attendre(409);
        post("/api/etablissements/" + ecole1 + "/inscriptions", jetonDirection, """
                {"anneeScolaireId":%d,"matriculeEleve":"%s"}""".formatted(anneeId, matricule)).attendre(409);
    }

    @Test
    @Order(5)
    void lAcceptationRespecteLaCapaciteDeLaClasse() throws Exception {
        Reponse acceptee = post("/api/inscriptions/" + inscriptionId + "/decision", jetonDirection, """
                {"decision":"ACCEPTEE","classeId":%d}""".formatted(classeId)).attendre(200);
        assertThat(acceptee.texte("$.statut")).isEqualTo("ACCEPTEE");
        assertThat(acceptee.texte("$.classeNom")).isEqualTo("A");

        Reponse second = post("/api/etablissements/" + ecole1 + "/inscriptions", jetonDirection, """
                {"anneeScolaireId":%d,"nouvelEleve":{"nom":"Durand","prenom":"Lea","dateNaissance":"2015-08-12","sexe":"F"}}"""
                .formatted(anneeId)).attendre(201);
        eleve2Id = second.id("$.eleve.id");
        long secondeInscription = second.id("$.id");

        post("/api/inscriptions/" + secondeInscription + "/decision", jetonDirection, """
                {"decision":"ACCEPTEE","classeId":%d}""".formatted(classeId)).attendre(409); // classe complète
        post("/api/inscriptions/" + secondeInscription + "/decision", jetonDirection, """
                {"decision":"REFUSEE"}""").attendre(200);
    }

    @Test
    @Order(6)
    void unEnseignantDoitChangerSonMotDePasseInitial() throws Exception {
        enseignantId = post("/api/etablissements/" + ecole1 + "/enseignants", jetonDirection, """
                {"email":"%s","nom":"Martin","prenom":"Alice","motDePasseInitial":"%s"}"""
                .formatted(EMAIL_ENSEIGNANT, MDP_INITIAL)).attendre(201).id("$.id");
        jetonEnseignant = premiereConnexion(EMAIL_ENSEIGNANT);
    }

    @Test
    @Order(7)
    void laDirectionAffecteLEnseignantALaClasse() throws Exception {
        String affectation = """
                {"enseignantId":%d,"matiereId":%d}""".formatted(enseignantId, matiereId);
        post("/api/classes/" + classeId + "/enseignants", jetonEnseignant, affectation).attendre(403);
        post("/api/classes/" + classeId + "/enseignants", jetonDirection, affectation).attendre(201);
        post("/api/classes/" + classeId + "/enseignants", jetonDirection, affectation).attendre(409);
    }

    @Test
    @Order(8)
    void lEnseignantFaitLAppel() throws Exception {
        Reponse feuille = put("/api/classes/" + classeId + "/presences/" + aujourdhui, jetonEnseignant, """
                {"lignes":[{"inscriptionId":%d,"statut":"ABSENT","motif":"Maladie","justifie":true}]}"""
                .formatted(inscriptionId)).attendre(200);
        assertThat(feuille.texte("$.lignes[0].statut")).isEqualTo("ABSENT");

        put("/api/classes/" + classeId + "/presences/" + aujourdhui.plusDays(1), jetonEnseignant, """
                {"lignes":[{"inscriptionId":%d,"statut":"PRESENT"}]}""".formatted(inscriptionId)).attendre(400);

        get("/api/classes/" + classeId + "/presences?date=" + aujourdhui, jetonDirection).attendre(200);
        get("/api/classes/" + classeId + "/presences?date=" + aujourdhui, jetonMinistere).attendre(200);
    }

    @Test
    @Order(9)
    void lEnseignantNoteEtToutChangementEstJournalise() throws Exception {
        evaluationId = post("/api/classes/" + classeId + "/evaluations", jetonEnseignant, """
                {"matiereId":%d,"periodeId":%d,"titre":"Contrôle 1","dateEvaluation":"%s"}"""
                .formatted(matiereId, periodeId, aujourdhui.minusDays(1))).attendre(201).id("$.id");

        String notes = """
                {"lignes":[{"inscriptionId":%d,"valeur":%s}]}""";
        put("/api/evaluations/" + evaluationId + "/notes", jetonDirection, notes.formatted(inscriptionId, "12"))
                .attendre(403); // la direction ne note pas
        put("/api/evaluations/" + evaluationId + "/notes", jetonEnseignant, notes.formatted(inscriptionId, "25"))
                .attendre(400); // au-dessus du barème
        Reponse premiere = put("/api/evaluations/" + evaluationId + "/notes", jetonEnseignant,
                notes.formatted(inscriptionId, "14")).attendre(200);
        assertThat(premiere.nombre("$.lignes[0].valeur")).isEqualTo(14.0);
        put("/api/evaluations/" + evaluationId + "/notes", jetonEnseignant, notes.formatted(inscriptionId, "16"))
                .attendre(200);

        assertThat(jdbc.queryForObject(
                "select count(*) from journal_audit where action = 'MODIFICATION_NOTE'", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "select details->>'avant' from journal_audit where action = 'MODIFICATION_NOTE'", String.class))
                .isEqualTo("14.00");
        assertThat(jdbc.queryForObject(
                "select details->>'apres' from journal_audit where action = 'MODIFICATION_NOTE'", String.class))
                .isEqualTo("16.00");
    }

    @Test
    @Order(10)
    void lesMoyennesEtLeRangSontCalcules() throws Exception {
        Reponse resultats = get("/api/classes/" + classeId + "/resultats?periodeId=" + periodeId, jetonDirection)
                .attendre(200);
        assertThat(resultats.lire("$.eleves.length()")).isEqualTo(1); // le second élève a été refusé
        assertThat(resultats.nombre("$.eleves[0].moyenneGenerale")).isEqualTo(16.0);
        assertThat(resultats.nombre("$.eleves[0].rang")).isEqualTo(1.0);
        assertThat(resultats.nombre("$.moyenneGeneraleClasse")).isEqualTo(16.0);
    }

    @Test
    @Order(11)
    void leParentNeConsulteQueSonEnfant() throws Exception {
        post("/api/eleves/" + eleveId + "/parents", jetonDirection, """
                {"email":"%s","nom":"Dupont","prenom":"Marie","relation":"mère","responsableLegal":true,"motDePasseInitial":"%s"}"""
                .formatted(EMAIL_PARENT, MDP_INITIAL)).attendre(201);
        jetonParent = premiereConnexion(EMAIL_PARENT);

        Reponse bulletin = get("/api/eleves/" + eleveId + "/bulletin?periodeId=" + periodeId, jetonParent)
                .attendre(200);
        assertThat(bulletin.nombre("$.moyenneGenerale")).isEqualTo(16.0);
        assertThat(bulletin.nombre("$.rang")).isEqualTo(1.0);
        assertThat(get("/api/eleves/" + eleveId + "/presences", jetonParent).attendre(200)
                .texte("$[0].statut")).isEqualTo("ABSENT");

        get("/api/eleves/" + eleve2Id, jetonParent).attendre(403);
        get("/api/classes/" + classeId + "/resultats?periodeId=" + periodeId, jetonParent).attendre(403);
        get("/api/classes/" + classeId + "/presences", jetonParent).attendre(403);
        get("/api/classes/" + classeId + "/evaluations", jetonParent).attendre(403);
    }

    @Test
    @Order(12)
    void leMinistereConsulteSansModifier() throws Exception {
        get("/api/eleves/" + eleveId, jetonMinistere).attendre(200);
        put("/api/eleves/" + eleveId, jetonMinistere, """
                {"nom":"X","prenom":"Y","dateNaissance":"2015-05-04","sexe":"M"}""").attendre(403);
    }

    @Test
    @Order(13)
    void cinqEchecsVerrouillentLeCompte() throws Exception {
        for (int i = 0; i < 5; i++) {
            connecter(EMAIL_PARENT, "mauvais-mot-de-passe").attendre(401);
        }
        connecter(EMAIL_PARENT, MDP_NOUVEAU).attendre(401);   // verrouillé, même avec le bon mot de passe
        get("/api/me", jetonParent).attendre(401);            // et les jetons existants sont refusés

        jdbc.update("update utilisateur set verrouille_jusqu_a = null where email = ?", EMAIL_PARENT);
        connecter(EMAIL_PARENT, MDP_NOUVEAU).attendre(200);
    }
}
