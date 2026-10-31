package com.plateforme.scolaire.web;

import com.plateforme.scolaire.service.DossierCandidatureService;
import com.plateforme.scolaire.web.dto.DossiersCandidaturesDto;
import com.plateforme.scolaire.web.dto.PieceCandidatureRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Dossiers de candidature d'un établissement : consultation par le périmètre, saisie par l'établissement. */
@RestController
@RequiredArgsConstructor
public class DossierCandidatureController {

    private final DossierCandidatureService service;

    @GetMapping("/api/etablissements/{id}/examens/{examenId}/dossiers")
    @PreAuthorize("@perimetre.peutAccederEtablissement(#id)")
    public DossiersCandidaturesDto dossiers(@PathVariable Long id, @PathVariable Long examenId) {
        return service.dossiers(id, examenId);
    }

    @PutMapping("/api/etablissements/{id}/examens/{examenId}/candidats/{candidatId}/pieces/{type}")
    @PreAuthorize("@perimetre.peutGererEtablissement(#id)")
    public DossiersCandidaturesDto marquer(@PathVariable Long id, @PathVariable Long examenId, @PathVariable Long candidatId,
                                           @PathVariable String type, @Valid @RequestBody PieceCandidatureRequest r) {
        return service.marquer(id, examenId, candidatId, type, r);
    }
}
