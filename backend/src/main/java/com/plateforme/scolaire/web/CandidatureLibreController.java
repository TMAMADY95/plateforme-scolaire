package com.plateforme.scolaire.web;

import com.plateforme.scolaire.service.CandidatureLibreService;
import com.plateforme.scolaire.web.dto.CandidatDto;
import com.plateforme.scolaire.web.dto.CandidatLibreRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Candidature libre au BEPC et au baccalauréat : saisie par l'établissement qui reçoit le dossier. */
@RestController
@RequiredArgsConstructor
public class CandidatureLibreController {

    private final CandidatureLibreService service;

    @PostMapping("/api/etablissements/{id}/examens/{examenId}/candidats-libres")
    @PreAuthorize("@perimetre.peutGererEtablissement(#id)")
    @ResponseStatus(HttpStatus.CREATED)
    public CandidatDto inscrire(@PathVariable Long id, @PathVariable Long examenId, @Valid @RequestBody CandidatLibreRequest r) {
        return service.inscrire(id, examenId, r);
    }
}
