package com.plateforme.scolaire.web;

import com.plateforme.scolaire.security.DroitsExamen;
import com.plateforme.scolaire.service.InfrastructureService;
import com.plateforme.scolaire.web.dto.InfrastructureDto;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Carte scolaire : infrastructures et équipements des établissements, saisis par la direction et consultés par les structures de tutelle. */
@RestController
@RequiredArgsConstructor
public class InfrastructureController {

    private final InfrastructureService service;

    @GetMapping("/api/etablissements/{etablissementId}/infrastructure")
    @PreAuthorize("hasAnyRole('MINISTERE','DGECS','DIRECTION_REGIONALE','DIRECTION_LOCALE','DIRECTION_ETABLISSEMENT') and @perimetre.peutAccederEtablissement(#etablissementId)")
    public InfrastructureDto fiche(@PathVariable Long etablissementId, @RequestParam Long anneeId) {
        return service.fiche(etablissementId, anneeId);
    }

    @PutMapping("/api/etablissements/{etablissementId}/infrastructure")
    @PreAuthorize("hasRole('DIRECTION_ETABLISSEMENT') and @perimetre.peutGererEtablissement(#etablissementId)")
    public InfrastructureDto enregistrer(@PathVariable Long etablissementId, @RequestParam Long anneeId, @Valid @RequestBody InfrastructureDto.Requete q,
            Principal principal) {
        return service.enregistrer(etablissementId, anneeId, q, principal == null ? null : principal.getName());
    }

    @GetMapping("/api/infrastructures")
    @PreAuthorize(DroitsExamen.CONSULTATION)
    public InfrastructureDto.Synthese synthese(@RequestParam Long anneeId) {
        return service.synthese(anneeId);
    }
}
