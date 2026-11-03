package com.plateforme.scolaire.web;

import com.plateforme.scolaire.service.ComptesInstitutionnelsService;
import com.plateforme.scolaire.security.ZonesAdminService;
import com.plateforme.scolaire.web.dto.ActivationRequest;
import com.plateforme.scolaire.web.dto.CompteInstitutionnelDto;
import com.plateforme.scolaire.web.dto.ReinitialisationRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Comptes des directions préfectorales et communales (DPE/DCE), administrés par l'IRE de leur région. */
@RestController
@RequiredArgsConstructor
public class ComptesLocauxController {

    private static final String DROIT = "hasRole('DIRECTION_REGIONALE') and @zonesAdmin.peutGererCompteLocal(#id)";

    private final ZonesAdminService zonesAdmin;
    private final ComptesInstitutionnelsService comptes;

    @GetMapping("/api/region/comptes-locaux")
    @PreAuthorize("hasRole('DIRECTION_REGIONALE')")
    public List<CompteInstitutionnelDto> lister() {
        return zonesAdmin.locauxDeMaRegion();
    }

    @PostMapping("/api/region/comptes-locaux/{id}/activation")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(DROIT)
    public void activer(@PathVariable Long id, @RequestBody ActivationRequest r) {
        comptes.activer(id, r.actif());
    }

    @PostMapping("/api/region/comptes-locaux/{id}/reinitialisation")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(DROIT)
    public void reinitialiser(@PathVariable Long id, @Valid @RequestBody ReinitialisationRequest r) {
        comptes.reinitialiser(id, r.motDePasseTemporaire());
    }
}
