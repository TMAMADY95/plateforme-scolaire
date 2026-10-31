package com.plateforme.scolaire.web;

import com.plateforme.scolaire.service.ConformiteService;
import com.plateforme.scolaire.service.InspectionService;
import com.plateforme.scolaire.web.dto.AutorisationRequest;
import com.plateforme.scolaire.web.dto.ConformiteDto;
import com.plateforme.scolaire.web.dto.InspectionDto;
import com.plateforme.scolaire.web.dto.InspectionRequest;
import com.plateforme.scolaire.web.dto.SignalementDto;
import com.plateforme.scolaire.web.dto.SignalementRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Conformité, inspections et sécurité : ministère (tout le pays) et directions régionales (leurs zones). */
@RestController
@RequiredArgsConstructor
public class ConformiteController {

    private static final String SUPERVISEUR = "hasAnyRole('MINISTERE','DIRECTION_REGIONALE') "
            + "and @perimetre.peutAccederEtablissement(#id)";

    private final ConformiteService conformiteService;
    private final InspectionService inspectionService;

    @GetMapping("/api/ministere/conformite")
    @PreAuthorize("hasRole('MINISTERE')")
    public ConformiteDto nationale(@RequestParam(required = false) Long zoneId,
                                   @RequestParam(required = false) String statut,
                                   @RequestParam(required = false) String secteur,
                                   @RequestParam(required = false) Long cycleId) {
        return conformiteService.nationale(zoneId, statut, secteur, cycleId);
    }

    @GetMapping("/api/region/conformite")
    @PreAuthorize("hasRole('DIRECTION_REGIONALE')")
    public ConformiteDto regionale(@RequestParam(required = false) String statut,
                                   @RequestParam(required = false) String secteur,
                                   @RequestParam(required = false) Long cycleId) {
        return conformiteService.regionale(statut, secteur, cycleId);
    }

    @GetMapping("/api/etablissements/{id}/inspections")
    @PreAuthorize(SUPERVISEUR)
    public List<InspectionDto> inspections(@PathVariable Long id) {
        return inspectionService.inspections(id);
    }

    @PostMapping("/api/etablissements/{id}/inspections")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(SUPERVISEUR)
    public InspectionDto inspecter(@PathVariable Long id, @Valid @RequestBody InspectionRequest r) {
        return inspectionService.inspecter(id, r);
    }

    @GetMapping("/api/etablissements/{id}/signalements-securite")
    @PreAuthorize(SUPERVISEUR)
    public List<SignalementDto> signalements(@PathVariable Long id) {
        return inspectionService.signalements(id);
    }

    @PostMapping("/api/etablissements/{id}/signalements-securite")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(SUPERVISEUR)
    public SignalementDto signaler(@PathVariable Long id, @Valid @RequestBody SignalementRequest r) {
        return inspectionService.signaler(id, r);
    }

    /** L'accès à l'établissement du signalement est vérifié par le service. */
    @PostMapping("/api/signalements-securite/{id}/resolution")
    @PreAuthorize("hasAnyRole('MINISTERE','DIRECTION_REGIONALE')")
    public SignalementDto resoudre(@PathVariable Long id) {
        return inspectionService.resoudre(id);
    }

    @PutMapping("/api/etablissements/{id}/autorisation")
    @PreAuthorize("hasRole('MINISTERE') and @perimetre.peutAccederEtablissement(#id)")
    public void autorisation(@PathVariable Long id, @Valid @RequestBody AutorisationRequest r) {
        inspectionService.modifierAutorisation(id, r);
    }
}
