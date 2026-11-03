package com.plateforme.scolaire.web;

import com.plateforme.scolaire.web.dto.ControleCoherenceDto;
import com.plateforme.scolaire.service.ControleCoherenceExamenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** Contrôle de cohérence des listes et des notes d'un établissement (lecture seule). */
@RestController
@RequiredArgsConstructor
public class ControleCoherenceExamenController {

    private final ControleCoherenceExamenService service;

    @GetMapping("/api/etablissements/{id}/examens/{examenId}/controle-coherence")
    @PreAuthorize("@perimetre.peutAccederEtablissement(#id)")
    public ControleCoherenceDto controle(@PathVariable Long id, @PathVariable Long examenId) {
        return service.controle(id, examenId);
    }
}
