package com.plateforme.scolaire.web;

import com.plateforme.scolaire.service.PeriodesEtablissementService;
import com.plateforme.scolaire.web.dto.GenerationPeriodesRequest;
import com.plateforme.scolaire.web.dto.PeriodeDto;
import com.plateforme.scolaire.web.dto.PeriodeEtablissementRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Périodes (trimestres ou semestres) d'un établissement. Les droits de gestion sont vérifiés par le service. */
@RestController
@RequiredArgsConstructor
public class PeriodesEtablissementController {

    private final PeriodesEtablissementService service;

    @GetMapping("/api/etablissements/{id}/annees-scolaires/{anneeId}/periodes")
    @PreAuthorize("@perimetre.peutAccederEtablissement(#id)")
    public List<PeriodeDto> deLEtablissement(@PathVariable Long id, @PathVariable Long anneeId) {
        return service.deLEtablissement(id, anneeId);
    }

    /** Les périodes qui s'appliquent à la classe : celles de son établissement, sinon les nationales. */
    @GetMapping("/api/classes/{classeId}/periodes")
    @PreAuthorize("@perimetreEmploiDuTemps.peutConsulterClasse(#classeId)")
    public List<PeriodeDto> deLaClasse(@PathVariable Long classeId) {
        return service.deLaClasse(classeId);
    }

    @PostMapping("/api/etablissements/{id}/annees-scolaires/{anneeId}/periodes/generation")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    public List<PeriodeDto> generer(@PathVariable Long id, @PathVariable Long anneeId,
                                    @Valid @RequestBody GenerationPeriodesRequest r) {
        return service.generer(id, anneeId, r.type());
    }

    @PostMapping("/api/etablissements/{id}/annees-scolaires/{anneeId}/periodes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    public PeriodeDto creer(@PathVariable Long id, @PathVariable Long anneeId,
                            @Valid @RequestBody PeriodeEtablissementRequest r) {
        return service.creer(id, anneeId, r);
    }

    @PutMapping("/api/etablissements/periodes/{periodeId}")
    @PreAuthorize("isAuthenticated()")
    public PeriodeDto modifier(@PathVariable Long periodeId, @Valid @RequestBody PeriodeEtablissementRequest r) {
        return service.modifier(periodeId, r);
    }

    @DeleteMapping("/api/etablissements/periodes/{periodeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("isAuthenticated()")
    public void supprimer(@PathVariable Long periodeId) {
        service.supprimer(periodeId);
    }

    /** L'établissement abandonne ses périodes propres et retrouve celles du ministère. */
    @DeleteMapping("/api/etablissements/{id}/annees-scolaires/{anneeId}/periodes")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("isAuthenticated()")
    public void retablir(@PathVariable Long id, @PathVariable Long anneeId) {
        service.retablir(id, anneeId);
    }
}
