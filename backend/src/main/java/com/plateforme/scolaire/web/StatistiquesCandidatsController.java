package com.plateforme.scolaire.web;

import com.plateforme.scolaire.service.StatistiquesCandidatsService;
import com.plateforme.scolaire.web.dto.StatistiquesCandidatsDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Statistiques des candidats aux examens : DGECS et ministère (consultation). */
@RestController
@RequiredArgsConstructor
public class StatistiquesCandidatsController {

    private final StatistiquesCandidatsService service;

    @GetMapping("/api/dgecs/statistiques-candidats")
    @PreAuthorize("hasAnyRole('MINISTERE','DGECS')")
    public StatistiquesCandidatsDto statistiques(@RequestParam Long anneeScolaireId) {
        return service.statistiques(anneeScolaireId);
    }
}
