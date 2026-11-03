package com.plateforme.scolaire.web;

import com.plateforme.scolaire.service.ConformiteProgrammeService;
import com.plateforme.scolaire.web.dto.ConformiteProgrammeDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ConformiteProgrammeController {

    private final ConformiteProgrammeService service;

    /** Heures de l'emploi du temps de la classe comparées au programme officiel (indicatif, non bloquant). */
    @GetMapping("/api/classes/{classeId}/conformite-programme")
    @PreAuthorize("@perimetreEmploiDuTemps.peutConsulterClasse(#classeId)")
    public ConformiteProgrammeDto controler(@PathVariable Long classeId) {
        return service.controler(classeId);
    }
}
