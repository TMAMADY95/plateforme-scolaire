package com.plateforme.scolaire.web;

import com.plateforme.scolaire.service.ConsultationAuditService;
import com.plateforme.scolaire.web.dto.PageAuditDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Journal d'audit national : réservé au ministère. */
@RestController
@RequiredArgsConstructor
public class AuditController {

    private final ConsultationAuditService consultationAuditService;

    @GetMapping("/api/audit")
    @PreAuthorize("hasRole('MINISTERE')")
    public PageAuditDto rechercher(@RequestParam(defaultValue = "") String action,
                                   @RequestParam(defaultValue = "") String entite,
                                   @RequestParam(defaultValue = "") String eleve,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "25") int taille) {
        return consultationAuditService.rechercher(action, entite, eleve, page, taille);
    }
}
