package com.plateforme.scolaire.web;

import com.plateforme.scolaire.service.PostesService;
import com.plateforme.scolaire.web.dto.MutationRequest;
import com.plateforme.scolaire.web.dto.PostesRequest;
import com.plateforme.scolaire.web.dto.RapportPostesDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Postes d'enseignants, besoins et mutations : ministère (tout le pays) et directions régionales (leurs zones). */
@RestController
@RequiredArgsConstructor
public class PostesController {

    private final PostesService postes;

    @GetMapping("/api/ministere/postes")
    @PreAuthorize("hasRole('MINISTERE')")
    public RapportPostesDto nationale(@RequestParam Long anneeScolaireId, @RequestParam(required = false) Long zoneId) {
        return postes.nationale(anneeScolaireId, zoneId);
    }

    @GetMapping("/api/region/postes")
    @PreAuthorize("hasRole('DIRECTION_REGIONALE')")
    public RapportPostesDto regionale(@RequestParam Long anneeScolaireId) {
        return postes.regionale(anneeScolaireId);
    }

    @PutMapping("/api/etablissements/{id}/postes")
    @PreAuthorize("hasAnyRole('MINISTERE','DIRECTION_REGIONALE') and @perimetre.peutAccederEtablissement(#id)")
    public void definir(@PathVariable Long id, @Valid @RequestBody PostesRequest r) {
        postes.definirPostes(id, r.postes());
    }

    /** L'accès aux deux établissements est vérifié par le service. */
    @PostMapping("/api/enseignants/{id}/mutation")
    @PreAuthorize("hasAnyRole('MINISTERE','DIRECTION_REGIONALE')")
    public void muter(@PathVariable Long id, @Valid @RequestBody MutationRequest r) {
        postes.muter(id, r);
    }
}
