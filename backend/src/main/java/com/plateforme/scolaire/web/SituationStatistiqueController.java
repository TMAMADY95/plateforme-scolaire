package com.plateforme.scolaire.web;

import com.plateforme.scolaire.security.DroitsExamen;
import com.plateforme.scolaire.service.SituationStatistiqueService;
import com.plateforme.scolaire.web.dto.SituationStatistiqueDto;
import com.plateforme.scolaire.web.dto.SituationStatistiqueRequest;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Situations statistiques datées des examens : établies par la DGECS, consultées aussi par le ministère. */
@RestController
@RequiredArgsConstructor
public class SituationStatistiqueController {

    private final SituationStatistiqueService service;

    @GetMapping("/api/dgecs/situations")
    @PreAuthorize("hasAnyRole('MINISTERE','DGECS')")
    public List<SituationStatistiqueDto> liste(@RequestParam Long anneeScolaireId) {
        return service.liste(anneeScolaireId);
    }

    @PostMapping("/api/dgecs/situations")
    @PreAuthorize(DroitsExamen.NATIONAL)
    @ResponseStatus(HttpStatus.CREATED)
    public SituationStatistiqueDto etablir(@RequestParam Long anneeScolaireId, @Valid @RequestBody SituationStatistiqueRequest q, Principal principal) {
        return service.etablir(anneeScolaireId, q, principal == null ? null : principal.getName());
    }
}
