package com.plateforme.scolaire.web;

import com.plateforme.scolaire.service.ConseilClasseService;
import com.plateforme.scolaire.web.dto.ConseilClasseDto;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Conseil de classe de fin d'année : consultation par le personnel, décisions par la direction de l'établissement. */
@RestController
@RequiredArgsConstructor
public class ConseilClasseController {

    private final ConseilClasseService service;

    @GetMapping("/api/classes/{classeId}/conseil")
    @PreAuthorize("hasAnyRole('DIRECTION_ETABLISSEMENT','PERSONNEL_ADMIN','DIRECTION_LOCALE','DIRECTION_REGIONALE','DGECS','MINISTERE') and @perimetre.peutAccederClasse(#classeId)")
    public ConseilClasseDto conseil(@PathVariable Long classeId) {
        return service.conseil(classeId);
    }

    @PutMapping("/api/classes/{classeId}/conseil/decisions")
    @PreAuthorize("hasRole('DIRECTION_ETABLISSEMENT') and @perimetre.peutGererClasse(#classeId)")
    public ConseilClasseDto decider(@PathVariable Long classeId, @Valid @RequestBody ConseilClasseDto.Requete q, Principal principal) {
        return service.decider(classeId, q, principal == null ? null : principal.getName());
    }

    @PostMapping("/api/classes/{classeId}/conseil/suggestions")
    @PreAuthorize("hasRole('DIRECTION_ETABLISSEMENT') and @perimetre.peutGererClasse(#classeId)")
    public ConseilClasseDto appliquerSuggestions(@PathVariable Long classeId, Principal principal) {
        return service.appliquerSuggestions(classeId, principal == null ? null : principal.getName());
    }
}
