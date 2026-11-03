package com.plateforme.scolaire.web;

import com.plateforme.scolaire.security.DroitsExamen;
import com.plateforme.scolaire.service.ChefCentreService;
import com.plateforme.scolaire.web.dto.ChefCentreDto;
import com.plateforme.scolaire.web.dto.DirectionRequest;
import com.plateforme.scolaire.web.dto.MonCentreDto;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Chefs de centre : désignés par les directions dont dépend le centre, ils ne voient que leurs centres. */
@RestController
@RequiredArgsConstructor
public class ChefCentreController {

    private static final String DROIT = DroitsExamen.GESTION + " and @accesCentre.peut(#centreId)";

    private final ChefCentreService service;

    @GetMapping("/api/examens/{examenId}/centres/{centreId}/chefs")
    @PreAuthorize(DroitsExamen.CONSULTATION + " and @accesCentre.peut(#centreId)")
    public List<ChefCentreDto> chefs(@PathVariable Long examenId, @PathVariable Long centreId) {
        return service.chefs(examenId, centreId);
    }

    @PostMapping("/api/examens/{examenId}/centres/{centreId}/chefs")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(DROIT)
    public List<ChefCentreDto> designer(@PathVariable Long examenId, @PathVariable Long centreId,
                                        @Valid @RequestBody DirectionRequest r) {
        return service.designer(examenId, centreId, r);
    }

    @DeleteMapping("/api/examens/{examenId}/centres/{centreId}/chefs/{utilisateurId}")
    @PreAuthorize(DROIT)
    public List<ChefCentreDto> retirer(@PathVariable Long examenId, @PathVariable Long centreId, @PathVariable Long utilisateurId) {
        return service.retirer(examenId, centreId, utilisateurId);
    }

    @GetMapping("/api/chef-centre/centres")
    @PreAuthorize("hasRole('CHEF_CENTRE')")
    public List<MonCentreDto> mesCentres() {
        return service.mesCentres();
    }
}
