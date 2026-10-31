package com.plateforme.scolaire.web;

import com.plateforme.scolaire.service.CorrectionExamenService;
import com.plateforme.scolaire.web.dto.CommissionCorrectionRequest;
import com.plateforme.scolaire.web.dto.CorrectionExamenDto;
import com.plateforme.scolaire.web.dto.MembreCommissionRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Commissions de correction : ministère et directions régionales (le national reste au ministère, voir le service). */
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MINISTERE','DIRECTION_REGIONALE')")
public class CorrectionExamenController {

    private final CorrectionExamenService service;

    @GetMapping("/api/examens/{examenId}/correction")
    public CorrectionExamenDto correction(@PathVariable Long examenId) {
        return service.correction(examenId);
    }

    @PostMapping("/api/examens/{examenId}/correction/commissions")
    @ResponseStatus(HttpStatus.CREATED)
    public CorrectionExamenDto creer(@PathVariable Long examenId, @Valid @RequestBody CommissionCorrectionRequest r) {
        return service.creer(examenId, r);
    }

    @PostMapping("/api/examens/{examenId}/correction/sous-commissions")
    public CorrectionExamenDto generer(@PathVariable Long examenId) {
        return service.genererSousCommissions(examenId);
    }

    @PutMapping("/api/examens/{examenId}/correction/commissions/{commissionId}")
    public CorrectionExamenDto modifier(@PathVariable Long examenId, @PathVariable Long commissionId,
                                        @Valid @RequestBody CommissionCorrectionRequest r) {
        return service.modifier(examenId, commissionId, r);
    }

    @DeleteMapping("/api/examens/{examenId}/correction/commissions/{commissionId}")
    public CorrectionExamenDto supprimer(@PathVariable Long examenId, @PathVariable Long commissionId) {
        return service.supprimer(examenId, commissionId);
    }

    @PostMapping("/api/examens/{examenId}/correction/commissions/{commissionId}/membres")
    @ResponseStatus(HttpStatus.CREATED)
    public CorrectionExamenDto ajouterMembre(@PathVariable Long examenId, @PathVariable Long commissionId,
                                             @Valid @RequestBody MembreCommissionRequest r) {
        return service.ajouterMembre(examenId, commissionId, r);
    }

    @DeleteMapping("/api/examens/{examenId}/correction/commissions/{commissionId}/membres/{membreId}")
    public CorrectionExamenDto retirerMembre(@PathVariable Long examenId, @PathVariable Long commissionId, @PathVariable Long membreId) {
        return service.retirerMembre(examenId, commissionId, membreId);
    }

    @PostMapping("/api/examens/{examenId}/correction/commissions/{commissionId}/cloture")
    public CorrectionExamenDto cloturer(@PathVariable Long examenId, @PathVariable Long commissionId) {
        return service.cloturer(examenId, commissionId);
    }
}
