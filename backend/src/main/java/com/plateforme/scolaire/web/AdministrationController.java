package com.plateforme.scolaire.web;

import com.plateforme.scolaire.service.AdministrationService;
import com.plateforme.scolaire.web.dto.ComptePersonnelDto;
import com.plateforme.scolaire.web.dto.DirectionRequest;
import com.plateforme.scolaire.web.dto.EtablissementDto;
import com.plateforme.scolaire.web.dto.EtablissementRequest;
import com.plateforme.scolaire.web.dto.StatutEtablissementRequest;
import com.plateforme.scolaire.web.dto.SyntheseNationaleDto;
import com.plateforme.scolaire.web.dto.ZoneDto;
import com.plateforme.scolaire.web.dto.ZoneRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Administration nationale (Lot 6) : réservée au ministère. */
@RestController
@RequiredArgsConstructor
public class AdministrationController {

    private static final String MINISTERE = "hasRole('MINISTERE')";

    private final AdministrationService administrationService;

    @GetMapping("/api/zones")
    @PreAuthorize("hasAnyRole('MINISTERE', 'DIRECTION_REGIONALE')")
    public List<ZoneDto> zones() {
        return administrationService.zones();
    }

    @PostMapping("/api/zones")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(MINISTERE)
    public ZoneDto creerZone(@Valid @RequestBody ZoneRequest requete) {
        return administrationService.creerZone(requete);
    }

    @PutMapping("/api/zones/{id}")
    @PreAuthorize(MINISTERE)
    public ZoneDto modifierZone(@PathVariable Long id, @Valid @RequestBody ZoneRequest requete) {
        return administrationService.modifierZone(id, requete);
    }

    @DeleteMapping("/api/zones/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(MINISTERE)
    public void supprimerZone(@PathVariable Long id) {
        administrationService.supprimerZone(id);
    }

    @PostMapping("/api/zones/{id}/direction-regionale")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(MINISTERE)
    public ComptePersonnelDto creerDirectionRegionale(@PathVariable Long id,
                                                      @Valid @RequestBody DirectionRequest requete) {
        return administrationService.creerDirectionRegionale(id, requete);
    }

    @PostMapping("/api/etablissements")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(MINISTERE)
    public EtablissementDto creerEtablissement(@Valid @RequestBody EtablissementRequest requete) {
        return administrationService.creerEtablissement(requete);
    }

    @PutMapping("/api/etablissements/{id}")
    @PreAuthorize(MINISTERE)
    public EtablissementDto modifierEtablissement(@PathVariable Long id,
                                                  @Valid @RequestBody EtablissementRequest requete) {
        return administrationService.modifierEtablissement(id, requete);
    }

    @PostMapping("/api/etablissements/{id}/statut")
    @PreAuthorize(MINISTERE)
    public EtablissementDto changerStatut(@PathVariable Long id,
                                          @Valid @RequestBody StatutEtablissementRequest requete) {
        return administrationService.changerStatut(id, requete.statut());
    }

    @PostMapping("/api/etablissements/{id}/direction")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(MINISTERE)
    public ComptePersonnelDto creerDirection(@PathVariable Long id, @Valid @RequestBody DirectionRequest requete) {
        return administrationService.creerDirection(id, requete);
    }

    @GetMapping("/api/ministere/synthese")
    @PreAuthorize(MINISTERE)
    public SyntheseNationaleDto synthese(@RequestParam Long anneeScolaireId) {
        return administrationService.synthese(anneeScolaireId);
    }
}
