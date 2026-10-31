package com.plateforme.scolaire.web;

import com.plateforme.scolaire.service.DeliberationExamenService;
import com.plateforme.scolaire.web.dto.JuryDto;
import com.plateforme.scolaire.web.dto.JuryMembreRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Jury, délibération et proclamation des résultats d'un examen. Les règles fines sont dans le service. */
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MINISTERE','DIRECTION_REGIONALE')")
public class DeliberationExamenController {

    private final DeliberationExamenService service;

    @GetMapping("/api/examens/{examenId}/jury")
    public JuryDto jury(@PathVariable Long examenId) {
        return service.jury(examenId);
    }

    @PostMapping("/api/examens/{examenId}/jury")
    public JuryDto initialiser(@PathVariable Long examenId) {
        return service.initialiser(examenId);
    }

    @PutMapping("/api/examens/{examenId}/jury/membres/{membreId}")
    public JuryDto modifierMembre(@PathVariable Long examenId, @PathVariable Long membreId,
                                  @Valid @RequestBody JuryMembreRequest r) {
        return service.modifierMembre(examenId, membreId, r);
    }

    @PostMapping("/api/examens/{examenId}/deliberation")
    public JuryDto deliberer(@PathVariable Long examenId) {
        return service.deliberer(examenId);
    }

    @DeleteMapping("/api/examens/{examenId}/deliberation")
    public JuryDto rouvrir(@PathVariable Long examenId) {
        return service.rouvrir(examenId);
    }

    @PostMapping("/api/examens/{examenId}/proclamation")
    public JuryDto proclamer(@PathVariable Long examenId) {
        return service.proclamer(examenId);
    }
}
