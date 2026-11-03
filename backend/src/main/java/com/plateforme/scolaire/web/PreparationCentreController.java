package com.plateforme.scolaire.web;

import com.plateforme.scolaire.security.DroitsExamen;
import com.plateforme.scolaire.service.PreparationCentreService;
import com.plateforme.scolaire.web.dto.PreparationCentreDto;
import com.plateforme.scolaire.web.dto.PreparationCentreRequest;
import com.plateforme.scolaire.web.dto.ResumePreparationDto;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Préparation matérielle des centres : renseignée par le terrain, suivie par les directions. */
@RestController
@RequiredArgsConstructor
public class PreparationCentreController {

    private final PreparationCentreService service;

    @GetMapping("/api/examens/{examenId}/centres/{centreId}/preparation")
    @PreAuthorize(DroitsExamen.LECTURE_TERRAIN + " and @accesCentre.peut(#centreId)")
    public PreparationCentreDto preparation(@PathVariable Long examenId, @PathVariable Long centreId) {
        return service.preparation(examenId, centreId);
    }

    @PutMapping("/api/examens/{examenId}/centres/{centreId}/preparation")
    @PreAuthorize(DroitsExamen.TERRAIN + " and @accesCentre.peut(#centreId)")
    public PreparationCentreDto marquer(@PathVariable Long examenId, @PathVariable Long centreId,
                                        @Valid @RequestBody PreparationCentreRequest r) {
        return service.marquer(examenId, centreId, r);
    }

    @GetMapping("/api/examens/{examenId}/preparation-centres")
    @PreAuthorize(DroitsExamen.CONSULTATION)
    public List<ResumePreparationDto> suivi(@PathVariable Long examenId) {
        return service.suivi(examenId);
    }
}
