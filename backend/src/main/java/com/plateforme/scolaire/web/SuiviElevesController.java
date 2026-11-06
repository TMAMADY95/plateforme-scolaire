package com.plateforme.scolaire.web;

import com.plateforme.scolaire.security.DroitsExamen;
import com.plateforme.scolaire.service.SuiviElevesService;
import com.plateforme.scolaire.web.dto.SuiviElevesDto;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Abandon scolaire et vulnérabilités : listes nominatives réservées à la direction de l'établissement ; totaux pour le périmètre. */
@RestController
@RequiredArgsConstructor
public class SuiviElevesController {

    private final SuiviElevesService service;

    @GetMapping("/api/etablissements/{etablissementId}/suivi-eleves")
    @PreAuthorize("hasRole('DIRECTION_ETABLISSEMENT') and @perimetre.peutGererEtablissement(#etablissementId)")
    public SuiviElevesDto.Suivi suivi(@PathVariable Long etablissementId, @RequestParam Long anneeId) {
        return service.suivi(etablissementId, anneeId);
    }

    @PostMapping("/api/etablissements/{etablissementId}/abandons")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('DIRECTION_ETABLISSEMENT') and @perimetre.peutGererEtablissement(#etablissementId)")
    public SuiviElevesDto.Suivi declarer(@PathVariable Long etablissementId, @Valid @RequestBody SuiviElevesDto.DeclarationAbandon q, Principal principal) {
        return service.declarerAbandon(etablissementId, q, principal == null ? null : principal.getName());
    }

    @DeleteMapping("/api/abandons/{id}")
    @PreAuthorize("hasRole('DIRECTION_ETABLISSEMENT')")
    public SuiviElevesDto.Suivi annuler(@PathVariable Long id) {
        return service.annulerAbandon(id);
    }

    @PutMapping("/api/etablissements/{etablissementId}/eleves/{eleveId}/vulnerabilite")
    @PreAuthorize("hasRole('DIRECTION_ETABLISSEMENT') and @perimetre.peutGererEtablissement(#etablissementId)")
    public void vulnerabilite(@PathVariable Long etablissementId, @PathVariable Long eleveId, @Valid @RequestBody SuiviElevesDto.Vulnerabilite q, Principal principal) {
        service.enregistrerVulnerabilite(etablissementId, eleveId, q, principal == null ? null : principal.getName());
    }

    @GetMapping("/api/abandons-synthese")
    @PreAuthorize(DroitsExamen.CONSULTATION)
    public SuiviElevesDto.Synthese synthese(@RequestParam Long anneeId) {
        return service.synthese(anneeId);
    }
}
