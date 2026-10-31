package com.plateforme.scolaire.web;

import com.plateforme.scolaire.service.SujetsExamenService;
import com.plateforme.scolaire.web.dto.BanqueSujetsDto;
import com.plateforme.scolaire.web.dto.SujetExamenRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Banque de sujets : ministère et directions régionales (le national reste au ministère, voir le service). */
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MINISTERE','DIRECTION_REGIONALE')")
public class SujetsExamenController {

    private final SujetsExamenService service;

    @GetMapping("/api/examens/{examenId}/sujets")
    public BanqueSujetsDto banque(@PathVariable Long examenId) {
        return service.banque(examenId);
    }

    @PostMapping("/api/examens/{examenId}/epreuves/{epreuveId}/sujets")
    @ResponseStatus(HttpStatus.CREATED)
    public BanqueSujetsDto ajouter(@PathVariable Long examenId, @PathVariable Long epreuveId, @Valid @RequestBody SujetExamenRequest r) {
        return service.ajouter(examenId, epreuveId, r);
    }

    @DeleteMapping("/api/examens/{examenId}/sujets/{sujetId}")
    public BanqueSujetsDto supprimer(@PathVariable Long examenId, @PathVariable Long sujetId) {
        return service.supprimer(examenId, sujetId);
    }

    @PostMapping("/api/examens/{examenId}/sujets/{sujetId}/validation")
    public BanqueSujetsDto valider(@PathVariable Long examenId, @PathVariable Long sujetId) {
        return service.valider(examenId, sujetId);
    }

    @PostMapping("/api/examens/{examenId}/epreuves/{epreuveId}/tirage")
    public BanqueSujetsDto tirer(@PathVariable Long examenId, @PathVariable Long epreuveId) {
        return service.tirer(examenId, epreuveId);
    }

    @PostMapping("/api/examens/{examenId}/sujets/{sujetId}/compromission")
    public BanqueSujetsDto compromettre(@PathVariable Long examenId, @PathVariable Long sujetId) {
        return service.compromettre(examenId, sujetId);
    }
}
