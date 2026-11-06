package com.plateforme.scolaire.web;

import com.plateforme.scolaire.security.DroitsExamen;
import com.plateforme.scolaire.service.ManuelsService;
import com.plateforme.scolaire.web.dto.ManuelsDto;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Manuels scolaires : catalogue tenu par le ministère, dotation déclarée par chaque établissement, synthèse pour les structures de tutelle. */
@RestController
@RequiredArgsConstructor
public class ManuelsController {

    private final ManuelsService service;

    @GetMapping("/api/manuels")
    @PreAuthorize("hasAnyRole('MINISTERE','DGECS','DIRECTION_REGIONALE','DIRECTION_LOCALE','DIRECTION_ETABLISSEMENT')")
    public List<ManuelsDto.Manuel> catalogue() {
        return service.catalogue();
    }

    @PostMapping("/api/manuels")
    @PreAuthorize(DroitsExamen.MINISTERE)
    @ResponseStatus(HttpStatus.CREATED)
    public ManuelsDto.Manuel ajouter(@Valid @RequestBody ManuelsDto.NouveauManuel q) {
        return service.ajouter(q);
    }

    @DeleteMapping("/api/manuels/{id}")
    @PreAuthorize(DroitsExamen.MINISTERE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@PathVariable Long id) {
        service.supprimer(id);
    }

    @GetMapping("/api/etablissements/{etablissementId}/manuels")
    @PreAuthorize("hasAnyRole('MINISTERE','DGECS','DIRECTION_REGIONALE','DIRECTION_LOCALE','DIRECTION_ETABLISSEMENT') and @perimetre.peutAccederEtablissement(#etablissementId)")
    public ManuelsDto.Dotation dotation(@PathVariable Long etablissementId, @RequestParam Long anneeId) {
        return service.dotation(etablissementId, anneeId);
    }

    @PutMapping("/api/etablissements/{etablissementId}/manuels/{manuelId}")
    @PreAuthorize("hasRole('DIRECTION_ETABLISSEMENT') and @perimetre.peutGererEtablissement(#etablissementId)")
    public ManuelsDto.Dotation declarer(@PathVariable Long etablissementId, @PathVariable Long manuelId, @RequestParam Long anneeId,
            @Valid @RequestBody ManuelsDto.Quantites q, Principal principal) {
        return service.declarer(etablissementId, anneeId, manuelId, q, principal == null ? null : principal.getName());
    }

    @GetMapping("/api/manuels-synthese")
    @PreAuthorize(DroitsExamen.CONSULTATION)
    public ManuelsDto.Synthese synthese(@RequestParam Long anneeId) {
        return service.synthese(anneeId);
    }
}
