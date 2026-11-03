package com.plateforme.scolaire.web;

import com.plateforme.scolaire.security.DroitsExamen;
import com.plateforme.scolaire.service.SuiviListesService;
import com.plateforme.scolaire.web.dto.SuiviListesDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** Suivi des listes de candidats d'un examen sur le périmètre de l'utilisateur (IRE, DPE/DCE, DGECS, ministère). */
@RestController
@RequiredArgsConstructor
public class SuiviListesController {

    private final SuiviListesService service;

    @GetMapping("/api/examens/{examenId}/suivi-listes")
    @PreAuthorize(DroitsExamen.CONSULTATION)
    public SuiviListesDto suivi(@PathVariable Long examenId) {
        return service.suivi(examenId);
    }
}
