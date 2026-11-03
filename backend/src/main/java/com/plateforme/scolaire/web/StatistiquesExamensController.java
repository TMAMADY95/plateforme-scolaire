package com.plateforme.scolaire.web;

import com.plateforme.scolaire.service.StatistiquesExamensService;
import com.plateforme.scolaire.web.dto.StatistiquesExamensDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Statistiques des examens (salles et centres, niveaux d'examen, types d'établissement) : DGECS et ministère. */
@RestController
@RequiredArgsConstructor
public class StatistiquesExamensController {

    private final StatistiquesExamensService service;

    @GetMapping("/api/dgecs/statistiques-examens")
    @PreAuthorize("hasAnyRole('MINISTERE','DGECS')")
    public StatistiquesExamensDto statistiques(@RequestParam Long anneeScolaireId) {
        return service.statistiques(anneeScolaireId);
    }
}
