package com.plateforme.scolaire.web;

import com.plateforme.scolaire.service.JournalAlertes;
import com.plateforme.scolaire.service.PlanActionService;
import com.plateforme.scolaire.web.dto.AlerteEnvoyeeDto;
import com.plateforme.scolaire.web.dto.PlanActionDto;
import com.plateforme.scolaire.web.dto.PlanActionRequest;
import com.plateforme.scolaire.web.dto.PlanStatutRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Plans d'action après inspection, et journal des alertes envoyées. */
@RestController
@RequiredArgsConstructor
public class PlanActionController {

    private static final String SUPERVISEUR = "hasAnyRole('MINISTERE','DIRECTION_REGIONALE') "
            + "and @perimetre.peutAccederEtablissement(#id)";

    private final PlanActionService plans;
    private final JournalAlertes journalAlertes;

    /** Lecture : le ministère, la direction régionale du périmètre, et la direction de l'établissement. */
    @GetMapping("/api/etablissements/{id}/plans-action")
    @PreAuthorize("(" + SUPERVISEUR + ") or @perimetre.peutGererEtablissement(#id)")
    public List<PlanActionDto> deLEtablissement(@PathVariable Long id) {
        return plans.deLEtablissement(id);
    }

    @PostMapping("/api/etablissements/{id}/plans-action")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(SUPERVISEUR)
    public PlanActionDto creer(@PathVariable Long id, @Valid @RequestBody PlanActionRequest r) {
        return plans.creer(id, r);
    }

    /** Les droits (direction : en cours / terminé ; supervision : tout) sont vérifiés par le service. */
    @PostMapping("/api/plans-action/{id}/statut")
    @PreAuthorize("isAuthenticated()")
    public PlanActionDto statut(@PathVariable Long id, @Valid @RequestBody PlanStatutRequest r) {
        return plans.changerStatut(id, r);
    }

    @GetMapping("/api/ministere/plans-action")
    @PreAuthorize("hasRole('MINISTERE')")
    public List<PlanActionDto> nationale(@RequestParam(required = false) Long zoneId) {
        return plans.nationale(zoneId);
    }

    @GetMapping("/api/region/plans-action")
    @PreAuthorize("hasRole('DIRECTION_REGIONALE')")
    public List<PlanActionDto> regionale() {
        return plans.regionale();
    }

    /** SMS d'absence envoyés aux parents : seule la direction de l'établissement les voit (ni région, ni commune, ni ministère). */
    @GetMapping("/api/etablissements/{id}/alertes-parents")
    @PreAuthorize("@perimetre.peutGererEtablissement(#id)")
    public List<AlerteEnvoyeeDto> alertesParents(@PathVariable Long id) {
        return journalAlertes.absencesDeLEtablissement(id);
    }

    /** Journal national : alertes de niveau établissement uniquement (inspections, plans d'action, résultats d'examens). */
    @GetMapping("/api/ministere/alertes")
    @PreAuthorize("hasRole('MINISTERE')")
    public List<AlerteEnvoyeeDto> alertes() {
        return journalAlertes.recentes();
    }
}
