package com.plateforme.scolaire.web;

import com.plateforme.scolaire.service.PersonnelCentreService;
import com.plateforme.scolaire.web.dto.PersonnelCentreDto;
import com.plateforme.scolaire.web.dto.PersonnelCentreRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Personnel des centres d'examen : ministère et directions régionales. */
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MINISTERE','DIRECTION_REGIONALE')")
public class PersonnelCentreController {

    private final PersonnelCentreService service;

    @GetMapping("/api/examens/{examenId}/centres/{centreId}/personnel")
    public PersonnelCentreDto personnel(@PathVariable Long examenId, @PathVariable Long centreId) {
        return service.personnel(examenId, centreId);
    }

    @PostMapping("/api/examens/{examenId}/centres/{centreId}/personnel")
    @ResponseStatus(HttpStatus.CREATED)
    public PersonnelCentreDto ajouter(@PathVariable Long examenId, @PathVariable Long centreId,
                                      @Valid @RequestBody PersonnelCentreRequest r) {
        return service.ajouter(examenId, centreId, r);
    }

    @DeleteMapping("/api/examens/{examenId}/centres/{centreId}/personnel/{personneId}")
    public PersonnelCentreDto retirer(@PathVariable Long examenId, @PathVariable Long centreId, @PathVariable Long personneId) {
        return service.retirer(examenId, centreId, personneId);
    }
}
