package com.plateforme.scolaire.web;

import com.plateforme.scolaire.service.RentreeService;
import com.plateforme.scolaire.web.dto.RentreeDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Passage à l'année suivante : réinscription des élèves qui passent ou redoublent d'après le conseil de classe. */
@RestController
@RequiredArgsConstructor
public class RentreeController {

    private final RentreeService service;

    @GetMapping("/api/etablissements/{etablissementId}/rentree")
    @PreAuthorize("hasAnyRole('DIRECTION_ETABLISSEMENT','PERSONNEL_ADMIN','DIRECTION_LOCALE','DIRECTION_REGIONALE','DGECS','MINISTERE') and @perimetre.peutAccederEtablissement(#etablissementId)")
    public RentreeDto apercu(@PathVariable Long etablissementId, @RequestParam Long anneeId) {
        return service.apercu(etablissementId, anneeId);
    }

    @PostMapping("/api/etablissements/{etablissementId}/rentree/preparation")
    @PreAuthorize("hasRole('DIRECTION_ETABLISSEMENT') and @perimetre.peutGererEtablissement(#etablissementId)")
    public RentreeDto.Preparation preparer(@PathVariable Long etablissementId, @RequestParam Long anneeId) {
        return service.preparer(etablissementId, anneeId);
    }
}
