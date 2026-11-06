package com.plateforme.scolaire.web;

import com.plateforme.scolaire.service.TransfertEleveService;
import com.plateforme.scolaire.web.dto.TransfertEleveDto;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Transferts d'élèves en cours d'année entre établissements. */
@RestController
@RequiredArgsConstructor
public class TransfertEleveController {

    private final TransfertEleveService service;

    @GetMapping("/api/etablissements/{etablissementId}/transferts")
    @PreAuthorize("hasAnyRole('DIRECTION_ETABLISSEMENT','DIRECTION_LOCALE','DIRECTION_REGIONALE','DGECS','MINISTERE') and @perimetre.peutAccederEtablissement(#etablissementId)")
    public TransfertEleveDto lister(@PathVariable Long etablissementId, @RequestParam Long anneeId) {
        return service.lister(etablissementId, anneeId);
    }

    @PostMapping("/api/etablissements/{etablissementId}/transferts")
    @PreAuthorize("hasRole('DIRECTION_ETABLISSEMENT') and @perimetre.peutGererEtablissement(#etablissementId)")
    @ResponseStatus(HttpStatus.CREATED)
    public TransfertEleveDto.Transfert demander(@PathVariable Long etablissementId, @Valid @RequestBody TransfertEleveDto.Demande q, Principal principal) {
        return service.demander(etablissementId, q, principal == null ? null : principal.getName());
    }

    @PostMapping("/api/transferts/{id}/decision")
    @PreAuthorize("hasRole('DIRECTION_ETABLISSEMENT')")
    public TransfertEleveDto.Transfert decider(@PathVariable Long id, @Valid @RequestBody TransfertEleveDto.Decision q, Principal principal) {
        return service.decider(id, q, principal == null ? null : principal.getName());
    }

    @PostMapping("/api/transferts/{id}/annulation")
    @PreAuthorize("hasRole('DIRECTION_ETABLISSEMENT')")
    public TransfertEleveDto.Transfert annuler(@PathVariable Long id) {
        return service.annuler(id);
    }
}
