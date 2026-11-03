package com.plateforme.scolaire.web;

import com.plateforme.scolaire.security.DroitsExamen;
import com.plateforme.scolaire.service.ConcordanceRapportsService;
import com.plateforme.scolaire.web.dto.ConcordanceRapportsDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** Concordance des rapports de surveillance d'un centre avec les statuts des candidats (lecture seule). */
@RestController
@RequiredArgsConstructor
public class ConcordanceRapportsController {

    private final ConcordanceRapportsService service;

    @GetMapping("/api/examens/{examenId}/centres/{centreId}/concordance-rapports")
    @PreAuthorize(DroitsExamen.LECTURE_TERRAIN + " and @accesCentre.peut(#centreId)")
    public ConcordanceRapportsDto concordance(@PathVariable Long examenId, @PathVariable Long centreId) {
        return service.concordance(examenId, centreId);
    }
}
