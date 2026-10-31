package com.plateforme.scolaire.web;

import com.plateforme.scolaire.service.DeroulementEpreuvesService;
import com.plateforme.scolaire.web.dto.DeroulementCentreDto;
import com.plateforme.scolaire.web.dto.OuverturePliRequest;
import com.plateforme.scolaire.web.dto.RapportEpreuveRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Déroulement des épreuves dans un centre : ministère et directions régionales. */
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MINISTERE','DIRECTION_REGIONALE')")
public class DeroulementEpreuvesController {

    private final DeroulementEpreuvesService service;

    @GetMapping("/api/examens/{examenId}/centres/{centreId}/deroulement")
    public DeroulementCentreDto deroulement(@PathVariable Long examenId, @PathVariable Long centreId) {
        return service.deroulement(examenId, centreId);
    }

    @PostMapping("/api/examens/{examenId}/centres/{centreId}/epreuves/{epreuveId}/pli/avancement")
    public DeroulementCentreDto avancer(@PathVariable Long examenId, @PathVariable Long centreId, @PathVariable Long epreuveId) {
        return service.avancer(examenId, centreId, epreuveId);
    }

    @PostMapping("/api/examens/{examenId}/centres/{centreId}/epreuves/{epreuveId}/pli/ouverture")
    public DeroulementCentreDto ouvrir(@PathVariable Long examenId, @PathVariable Long centreId, @PathVariable Long epreuveId,
                                       @Valid @RequestBody OuverturePliRequest r) {
        return service.ouvrir(examenId, centreId, epreuveId, r);
    }

    @PutMapping("/api/examens/{examenId}/centres/{centreId}/epreuves/{epreuveId}/rapport")
    public DeroulementCentreDto rapport(@PathVariable Long examenId, @PathVariable Long centreId, @PathVariable Long epreuveId,
                                        @Valid @RequestBody RapportEpreuveRequest r) {
        return service.rapport(examenId, centreId, epreuveId, r);
    }
}
