package com.plateforme.scolaire.web;

import com.plateforme.scolaire.service.FusionEleveService;
import com.plateforme.scolaire.web.dto.FusionEleveDto;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Registre national des élèves : détection et fusion des dossiers en double (ministère et DGECS). */
@RestController
@RequiredArgsConstructor
public class FusionEleveController {

    private final FusionEleveService service;

    @GetMapping("/api/eleves-doublons")
    @PreAuthorize("hasAnyRole('MINISTERE','DGECS')")
    public FusionEleveDto doublons() {
        return service.doublons();
    }

    @PostMapping("/api/eleves/{conserveId}/fusion")
    @PreAuthorize("hasAnyRole('MINISTERE','DGECS')")
    public FusionEleveDto.Resultat fusionner(@PathVariable Long conserveId, @Valid @RequestBody FusionEleveDto.Requete q, Principal principal) {
        return service.fusionner(conserveId, q, principal == null ? null : principal.getName());
    }
}
