package com.plateforme.scolaire.web;

import com.plateforme.scolaire.service.JustificationAbsenceService;
import com.plateforme.scolaire.web.dto.DecisionJustificationRequest;
import com.plateforme.scolaire.web.dto.JustificationDto;
import com.plateforme.scolaire.web.dto.JustificationRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class JustificationAbsenceController {

    private final JustificationAbsenceService service;

    @GetMapping("/api/eleves/{id}/justifications")
    @PreAuthorize("@perimetre.peutAccederEleve(#id)")
    public List<JustificationDto> deLEleve(@PathVariable Long id) {
        return service.deLEleve(id);
    }

    /** Le service vérifie que l'absence concerne un élève rattaché à l'appelant. */
    @PostMapping("/api/presences/{id}/justification")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    public JustificationDto soumettre(@PathVariable Long id, @Valid @RequestBody JustificationRequest r) {
        return service.soumettre(id, r.motif());
    }

    @GetMapping("/api/etablissements/{id}/justifications")
    @PreAuthorize("@perimetre.peutGererEtablissement(#id)")
    public List<JustificationDto> enAttente(@PathVariable Long id) {
        return service.enAttente(id);
    }

    @PostMapping("/api/justifications/{id}/decision")
    @PreAuthorize("isAuthenticated()")
    public JustificationDto decider(@PathVariable Long id, @Valid @RequestBody DecisionJustificationRequest r) {
        return service.decider(id, r);
    }
}
