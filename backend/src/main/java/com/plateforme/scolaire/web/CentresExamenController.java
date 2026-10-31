package com.plateforme.scolaire.web;

import com.plateforme.scolaire.service.CentresExamenService;
import com.plateforme.scolaire.web.dto.CriteresCentreDto;
import com.plateforme.scolaire.web.dto.CriteresCentreRequest;
import com.plateforme.scolaire.web.dto.EtablissementCentreDto;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Conditions pour être centre d'examen : fixées par le ministère, lisibles par la supervision. */
@RestController
@RequiredArgsConstructor
public class CentresExamenController {

    private final CentresExamenService service;

    @GetMapping("/api/centres-examen/criteres")
    @PreAuthorize("hasAnyRole('MINISTERE','DIRECTION_REGIONALE')")
    public CriteresCentreDto criteres() {
        return service.criteres();
    }

    @PutMapping("/api/centres-examen/criteres")
    @PreAuthorize("hasRole('MINISTERE')")
    public CriteresCentreDto modifier(@Valid @RequestBody CriteresCentreRequest r) {
        return service.modifier(r);
    }

    @GetMapping("/api/centres-examen/etablissements")
    @PreAuthorize("hasAnyRole('MINISTERE','DIRECTION_REGIONALE')")
    public List<EtablissementCentreDto> etablissements() {
        return service.etablissements();
    }
}
