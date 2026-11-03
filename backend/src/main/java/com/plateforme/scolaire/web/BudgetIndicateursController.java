package com.plateforme.scolaire.web;

import com.plateforme.scolaire.security.DroitsExamen;
import com.plateforme.scolaire.service.BudgetExamenService;
import com.plateforme.scolaire.service.IndicateursDgecsService;
import com.plateforme.scolaire.web.dto.BudgetExamenDto;
import com.plateforme.scolaire.web.dto.BudgetExamenRequest;
import com.plateforme.scolaire.web.dto.IndicateursDgecsDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Budget des examens et indicateurs du directeur général : gérés par la DGECS, consultés par le ministère. */
@RestController
@RequiredArgsConstructor
public class BudgetIndicateursController {

    private final BudgetExamenService budget;
    private final IndicateursDgecsService indicateurs;

    @GetMapping("/api/examens/{examenId}/budget")
    @PreAuthorize("hasAnyRole('MINISTERE','DGECS')")
    public BudgetExamenDto budget(@PathVariable Long examenId) {
        return budget.budget(examenId);
    }

    @PutMapping("/api/examens/{examenId}/budget")
    @PreAuthorize(DroitsExamen.NATIONAL)
    public BudgetExamenDto enregistrer(@PathVariable Long examenId, @Valid @RequestBody BudgetExamenRequest q) {
        return budget.enregistrer(examenId, q);
    }

    @GetMapping("/api/dgecs/indicateurs")
    @PreAuthorize("hasAnyRole('MINISTERE','DGECS')")
    public IndicateursDgecsDto indicateurs(@RequestParam Long anneeScolaireId) {
        return indicateurs.indicateurs(anneeScolaireId);
    }
}
