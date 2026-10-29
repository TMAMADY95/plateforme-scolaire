package com.plateforme.scolaire.web;

import java.sql.Connection;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Point de contrôle pour le répartiteur de charge ou l'orchestrateur : 200 si le serveur répond et que la base est
 * joignable, 503 sinon. Public, ne renvoie aucun détail.
 */
@RestController
public class SanteController {

    private final DataSource dataSource;

    public SanteController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/api/sante")
    public ResponseEntity<Map<String, String>> sante() {
        try (Connection connexion = dataSource.getConnection()) {
            if (connexion.isValid(2)) {
                return ResponseEntity.ok(Map.of("statut", "UP"));
            }
        } catch (Exception e) {
            // base injoignable : réponse 503 ci-dessous, sans détail
        }
        return ResponseEntity.status(503).body(Map.of("statut", "DOWN"));
    }
}
