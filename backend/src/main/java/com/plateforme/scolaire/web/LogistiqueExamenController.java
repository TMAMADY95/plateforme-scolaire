package com.plateforme.scolaire.web;

import com.plateforme.scolaire.security.DroitsExamen;
import com.plateforme.scolaire.service.LogistiqueExamenService;
import com.plateforme.scolaire.web.dto.LivraisonCentreRequest;
import com.plateforme.scolaire.web.dto.LogistiqueExamenDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Préparation nationale des centres (personnel et fournitures) : gérée par la DGECS, consultée par le ministère. */
@RestController
@RequiredArgsConstructor
public class LogistiqueExamenController {

    private final LogistiqueExamenService service;

    @GetMapping("/api/examens/{examenId}/logistique")
    @PreAuthorize("hasAnyRole('MINISTERE','DGECS')")
    public LogistiqueExamenDto suivi(@PathVariable Long examenId) {
        return service.suivi(examenId);
    }

    @PutMapping("/api/examens/{examenId}/centres/{centreId}/logistique")
    @PreAuthorize(DroitsExamen.NATIONAL)
    public LogistiqueExamenDto enregistrer(@PathVariable Long examenId, @PathVariable Long centreId, @Valid @RequestBody LivraisonCentreRequest q) {
        return service.enregistrer(examenId, centreId, q);
    }
}
