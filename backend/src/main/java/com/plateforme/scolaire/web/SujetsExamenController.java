package com.plateforme.scolaire.web;

import com.plateforme.scolaire.security.DroitsExamen;
import com.plateforme.scolaire.service.SujetsExamenService;
import com.plateforme.scolaire.web.dto.BanqueSujetsDto;
import com.plateforme.scolaire.web.dto.SujetExamenRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Banque de sujets : consultation pour le ministère ; gestion par la DGECS, et par les directions de zone hors examen national. */
@RestController
@RequiredArgsConstructor
@PreAuthorize(DroitsExamen.CONSULTATION)
public class SujetsExamenController {

    private final SujetsExamenService service;

    @GetMapping("/api/examens/{examenId}/sujets")
    public BanqueSujetsDto banque(@PathVariable Long examenId) {
        return service.banque(examenId);
    }

    @PostMapping("/api/examens/{examenId}/epreuves/{epreuveId}/sujets")
    @PreAuthorize(DroitsExamen.GESTION)
    @ResponseStatus(HttpStatus.CREATED)
    public BanqueSujetsDto ajouter(@PathVariable Long examenId, @PathVariable Long epreuveId, @Valid @RequestBody SujetExamenRequest r) {
        return service.ajouter(examenId, epreuveId, r);
    }

    @DeleteMapping("/api/examens/{examenId}/sujets/{sujetId}")
    @PreAuthorize(DroitsExamen.GESTION)
    public BanqueSujetsDto supprimer(@PathVariable Long examenId, @PathVariable Long sujetId) {
        return service.supprimer(examenId, sujetId);
    }

    @PostMapping("/api/examens/{examenId}/sujets/{sujetId}/validation")
    @PreAuthorize(DroitsExamen.GESTION)
    public BanqueSujetsDto valider(@PathVariable Long examenId, @PathVariable Long sujetId) {
        return service.valider(examenId, sujetId);
    }

    @PostMapping("/api/examens/{examenId}/epreuves/{epreuveId}/tirage")
    @PreAuthorize(DroitsExamen.GESTION)
    public BanqueSujetsDto tirer(@PathVariable Long examenId, @PathVariable Long epreuveId) {
        return service.tirer(examenId, epreuveId);
    }

    @PostMapping("/api/examens/{examenId}/sujets/{sujetId}/compromission")
    @PreAuthorize(DroitsExamen.GESTION)
    public BanqueSujetsDto compromettre(@PathVariable Long examenId, @PathVariable Long sujetId) {
        return service.compromettre(examenId, sujetId);
    }
}
