package com.plateforme.scolaire.web;

import com.plateforme.scolaire.service.RegionService;
import com.plateforme.scolaire.web.dto.SyntheseNationaleDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Vue de la direction régionale : lecture seule, limitée à ses zones. */
@RestController
@RequiredArgsConstructor
public class RegionController {

    private final RegionService regionService;

    @GetMapping("/api/region/synthese")
    @PreAuthorize("hasRole('DIRECTION_REGIONALE')")
    public SyntheseNationaleDto synthese(@RequestParam Long anneeScolaireId) {
        return regionService.synthese(anneeScolaireId);
    }
}
