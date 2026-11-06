package com.plateforme.scolaire.web;

import com.plateforme.scolaire.service.DisciplineService;
import com.plateforme.scolaire.web.dto.DisciplineDto;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Discipline scolaire : réservée à la direction de l'établissement ; les parents voient les sanctions de leur enfant. */
@RestController
@RequiredArgsConstructor
public class DisciplineController {

    private final DisciplineService service;

    @GetMapping("/api/etablissements/{etablissementId}/discipline")
    @PreAuthorize("hasRole('DIRECTION_ETABLISSEMENT') and @perimetre.peutGererEtablissement(#etablissementId)")
    public DisciplineDto lister(@PathVariable Long etablissementId, @RequestParam Long anneeId) {
        return service.lister(etablissementId, anneeId);
    }

    @PostMapping("/api/etablissements/{etablissementId}/discipline")
    @PreAuthorize("hasRole('DIRECTION_ETABLISSEMENT') and @perimetre.peutGererEtablissement(#etablissementId)")
    @ResponseStatus(HttpStatus.CREATED)
    public DisciplineDto.Incident signaler(@PathVariable Long etablissementId, @Valid @RequestBody DisciplineDto.Signalement q, Principal principal) {
        return service.signaler(etablissementId, q, principal == null ? null : principal.getName());
    }

    @PutMapping("/api/discipline/{id}/sanction")
    @PreAuthorize("hasRole('DIRECTION_ETABLISSEMENT')")
    public DisciplineDto.Incident sanctionner(@PathVariable Long id, @Valid @RequestBody DisciplineDto.Sanction q, Principal principal) {
        return service.sanctionner(id, q, principal == null ? null : principal.getName());
    }

    @PostMapping("/api/discipline/{id}/classement")
    @PreAuthorize("hasRole('DIRECTION_ETABLISSEMENT')")
    public DisciplineDto.Incident classer(@PathVariable Long id, @Valid @RequestBody DisciplineDto.Classement q, Principal principal) {
        return service.classer(id, q, principal == null ? null : principal.getName());
    }

    @GetMapping("/api/eleves/{eleveId}/discipline")
    @PreAuthorize("hasAnyRole('DIRECTION_ETABLISSEMENT','PARENT','ELEVE') and @perimetre.peutAccederEleve(#eleveId)")
    public List<DisciplineDto.Incident> parEleve(@PathVariable Long eleveId) {
        return service.parEleve(eleveId);
    }
}
