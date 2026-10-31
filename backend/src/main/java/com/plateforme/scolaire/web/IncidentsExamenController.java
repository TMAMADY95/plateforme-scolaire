package com.plateforme.scolaire.web;

import com.plateforme.scolaire.service.IncidentsExamenService;
import com.plateforme.scolaire.web.dto.IncidentExamenRequest;
import com.plateforme.scolaire.web.dto.IncidentsCentreDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Incidents des centres d'examen : ministère et directions régionales. */
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MINISTERE','DIRECTION_REGIONALE')")
public class IncidentsExamenController {

    private final IncidentsExamenService service;

    @GetMapping("/api/examens/{examenId}/centres/{centreId}/incidents")
    public IncidentsCentreDto incidents(@PathVariable Long examenId, @PathVariable Long centreId) {
        return service.incidents(examenId, centreId);
    }

    @PostMapping("/api/examens/{examenId}/centres/{centreId}/incidents")
    @ResponseStatus(HttpStatus.CREATED)
    public IncidentsCentreDto signaler(@PathVariable Long examenId, @PathVariable Long centreId,
                                       @Valid @RequestBody IncidentExamenRequest r) {
        return service.signaler(examenId, centreId, r);
    }

    @PostMapping("/api/examens/{examenId}/centres/{centreId}/incidents/{incidentId}/traitement")
    public IncidentsCentreDto traiter(@PathVariable Long examenId, @PathVariable Long centreId, @PathVariable Long incidentId,
                                      @RequestBody java.util.Map<String, String> corps) {
        return service.traiter(examenId, centreId, incidentId, corps.get("decision"));
    }
}
