package com.plateforme.scolaire.web;

import com.plateforme.scolaire.security.DroitsExamen;
import com.plateforme.scolaire.service.CalendrierPrevisionnelService;
import com.plateforme.scolaire.web.dto.CalendrierPrevisionnelDto;
import com.plateforme.scolaire.web.dto.CalendrierPrevisionnelRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Calendrier prévisionnel de la correction, de la délibération et de la proclamation. */
@RestController
@RequiredArgsConstructor
public class CalendrierPrevisionnelController {

    private final CalendrierPrevisionnelService service;

    @GetMapping("/api/examens/{examenId}/calendrier-previsionnel")
    @PreAuthorize("isAuthenticated()")
    public CalendrierPrevisionnelDto calendrier(@PathVariable Long examenId) {
        return service.calendrier(examenId);
    }

    /** Mêmes droits que la période des épreuves : la DGECS, et les directions de zone pour leurs examens (contrôle fait par le service). */
    @PutMapping("/api/examens/{examenId}/calendrier-previsionnel")
    @PreAuthorize(DroitsExamen.GESTION)
    public CalendrierPrevisionnelDto definir(@PathVariable Long examenId, @RequestBody CalendrierPrevisionnelRequest r) {
        return service.definir(examenId, r);
    }
}
