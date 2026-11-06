package com.plateforme.scolaire.web;

import com.plateforme.scolaire.security.DroitsExamen;
import com.plateforme.scolaire.service.CarteScolaireService;
import com.plateforme.scolaire.web.dto.CarteScolaireDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CarteScolaireController {

    private final CarteScolaireService service;

    @GetMapping("/api/carte-scolaire")
    @PreAuthorize(DroitsExamen.CONSULTATION)
    public CarteScolaireDto carte(@RequestParam Long anneeId, @RequestParam(defaultValue = "50") int normeElevesParSalle,
            @RequestParam(defaultValue = "40") int normeElevesParEnseignant) {
        return service.calculer(anneeId, normeElevesParSalle, normeElevesParEnseignant);
    }
}
