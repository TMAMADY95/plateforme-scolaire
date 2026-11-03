package com.plateforme.scolaire.web;

import com.plateforme.scolaire.security.DroitsExamen;
import com.plateforme.scolaire.service.AmenagementCandidatService;
import com.plateforme.scolaire.service.EtatCentresService;
import com.plateforme.scolaire.web.dto.AmenagementCandidatRequest;
import com.plateforme.scolaire.web.dto.CaracteristiquesCentreDto;
import com.plateforme.scolaire.web.dto.EtatCentresDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Caractéristiques des centres d'examen, synthèse nationale de leur état et aménagements des candidats en situation de handicap. */
@RestController
@RequiredArgsConstructor
public class EtatCentresController {

    private final EtatCentresService service;
    private final AmenagementCandidatService amenagements;

    @GetMapping("/api/examens/{examenId}/centres/{centreId}/caracteristiques")
    @PreAuthorize(DroitsExamen.LECTURE_TERRAIN + " and @accesCentre.peut(#centreId)")
    public CaracteristiquesCentreDto caracteristiques(@PathVariable Long examenId, @PathVariable Long centreId) {
        return service.caracteristiques(examenId, centreId);
    }

    @PutMapping("/api/examens/{examenId}/centres/{centreId}/caracteristiques")
    @PreAuthorize(DroitsExamen.TERRAIN + " and @accesCentre.peut(#centreId)")
    public CaracteristiquesCentreDto enregistrer(@PathVariable Long examenId, @PathVariable Long centreId, @Valid @RequestBody CaracteristiquesCentreDto q) {
        return service.enregistrer(examenId, centreId, q);
    }

    @GetMapping("/api/dgecs/etat-centres")
    @PreAuthorize("hasAnyRole('MINISTERE','DGECS')")
    public EtatCentresDto synthese(@RequestParam Long anneeScolaireId) {
        return service.synthese(anneeScolaireId);
    }

    @PutMapping("/api/candidats-examen/{id}/amenagement")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("isAuthenticated()")
    public void amenagement(@PathVariable Long id, @Valid @RequestBody AmenagementCandidatRequest q) {
        amenagements.definir(id, q);
    }
}
