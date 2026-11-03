package com.plateforme.scolaire.web;

import com.plateforme.scolaire.service.SuiviTraitementService;
import com.plateforme.scolaire.web.dto.SuiviTraitementDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Traitement des copies et fiabilité du processus : DGECS et ministère (consultation). */
@RestController
@RequiredArgsConstructor
public class SuiviTraitementController {

    private final SuiviTraitementService service;

    @GetMapping("/api/dgecs/suivi-traitement")
    @PreAuthorize("hasAnyRole('MINISTERE','DGECS')")
    public SuiviTraitementDto suivi(@RequestParam Long anneeScolaireId) {
        return service.suivi(anneeScolaireId);
    }
}
