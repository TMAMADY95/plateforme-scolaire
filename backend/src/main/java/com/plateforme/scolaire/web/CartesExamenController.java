package com.plateforme.scolaire.web;

import com.plateforme.scolaire.service.CartesExamenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** Cartes spéciales d'examen d'un établissement (PDF). */
@RestController
@RequiredArgsConstructor
public class CartesExamenController {

    private final CartesExamenService service;

    @GetMapping("/api/etablissements/{id}/examens/{examenId}/cartes.pdf")
    @PreAuthorize("@perimetre.peutAccederEtablissement(#id)")
    public ResponseEntity<byte[]> cartes(@PathVariable Long id, @PathVariable Long examenId) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"cartes-examen.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(service.cartes(id, examenId));
    }
}
