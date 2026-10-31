package com.plateforme.scolaire.web;

import com.plateforme.scolaire.service.ComptesInstitutionnelsService;
import com.plateforme.scolaire.web.dto.ActivationRequest;
import com.plateforme.scolaire.web.dto.CompteInstitutionnelDto;
import com.plateforme.scolaire.web.dto.DirectionRequest;
import com.plateforme.scolaire.web.dto.ReinitialisationRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Comptes du ministère et des directions régionales, gérés par le ministère. */
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('MINISTERE')")
public class ComptesInstitutionnelsController {

    private final ComptesInstitutionnelsService comptes;

    @GetMapping("/api/ministere/comptes")
    public List<CompteInstitutionnelDto> lister() {
        return comptes.lister();
    }

    @PostMapping("/api/ministere/comptes")
    @ResponseStatus(HttpStatus.CREATED)
    public CompteInstitutionnelDto creer(@Valid @RequestBody DirectionRequest r) {
        return comptes.creerMinistere(r);
    }

    @PostMapping("/api/ministere/comptes/{id}/activation")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void activer(@PathVariable Long id, @RequestBody ActivationRequest r) {
        comptes.activer(id, r.actif());
    }

    @PostMapping("/api/ministere/comptes/{id}/reinitialisation")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reinitialiser(@PathVariable Long id, @Valid @RequestBody ReinitialisationRequest r) {
        comptes.reinitialiser(id, r.motDePasseTemporaire());
    }
}
