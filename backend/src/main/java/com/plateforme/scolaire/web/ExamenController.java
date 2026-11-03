package com.plateforme.scolaire.web;

import com.plateforme.scolaire.security.DroitsExamen;
import com.plateforme.scolaire.service.ExamenService;
import com.plateforme.scolaire.web.dto.ExamenDto;
import com.plateforme.scolaire.web.dto.ExamenRequest;
import com.plateforme.scolaire.web.dto.RapportExamenDto;
import com.plateforme.scolaire.web.dto.ResultatExamenRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Examens nationaux : création par le ministère, saisie et lecture dans le périmètre de chacun. */
@RestController
@RequiredArgsConstructor
public class ExamenController {

    private final ExamenService examenService;
    private final com.plateforme.scolaire.service.TypeExamenService typesExamen;

    @GetMapping("/api/types-examen")
    @PreAuthorize(DroitsExamen.CONSULTATION)
    public List<com.plateforme.scolaire.web.dto.TypeExamenDto> types() {
        return typesExamen.lister();
    }

    @PostMapping("/api/types-examen")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(DroitsExamen.NATIONAL)
    public com.plateforme.scolaire.web.dto.TypeExamenDto creerType(
            @Valid @RequestBody com.plateforme.scolaire.web.dto.TypeExamenRequest r) {
        return typesExamen.creer(r);
    }

    @PutMapping("/api/types-examen/{id}")
    @PreAuthorize(DroitsExamen.NATIONAL)
    public com.plateforme.scolaire.web.dto.TypeExamenDto modifierType(
            @PathVariable Long id, @Valid @RequestBody com.plateforme.scolaire.web.dto.TypeExamenRequest r) {
        return typesExamen.modifier(id, r);
    }

    @DeleteMapping("/api/types-examen/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(DroitsExamen.NATIONAL)
    public void supprimerType(@PathVariable Long id) {
        typesExamen.supprimer(id);
    }

    /** NATIONAL et les types de zone du référentiel : les niveaux qui peuvent organiser un examen. */
    @GetMapping("/api/examens/niveaux-organisation")
    @PreAuthorize(DroitsExamen.CONSULTATION)
    public List<String> niveauxOrganisation() {
        return examenService.niveauxOrganisation();
    }

    @GetMapping("/api/examens")
    @PreAuthorize(DroitsExamen.CONSULTATION)
    public List<ExamenDto> lister(@RequestParam Long anneeScolaireId) {
        return examenService.lister(anneeScolaireId);
    }

    @PostMapping("/api/examens")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(DroitsExamen.NATIONAL)
    public ExamenDto creer(@Valid @RequestBody ExamenRequest r) {
        return examenService.creer(r);
    }

    @GetMapping("/api/ministere/examens/{examenId}")
    @PreAuthorize("hasAnyRole('MINISTERE','DGECS')")
    public RapportExamenDto nationale(@PathVariable Long examenId, @RequestParam(required = false) Long zoneId) {
        return examenService.nationale(examenId, zoneId);
    }

    @GetMapping("/api/region/examens/{examenId}")
    @PreAuthorize("hasAnyRole('DIRECTION_REGIONALE','DIRECTION_LOCALE')")
    public RapportExamenDto regionale(@PathVariable Long examenId) {
        return examenService.regionale(examenId);
    }

    @PutMapping("/api/examens/{examenId}/resultats/{etablissementId}")
    @PreAuthorize(DroitsExamen.GESTION + " and @perimetre.peutAccederEtablissement(#etablissementId)")
    public void saisir(@PathVariable Long examenId, @PathVariable Long etablissementId,
                       @Valid @RequestBody ResultatExamenRequest r) {
        examenService.saisir(examenId, etablissementId, r);
    }

    @PutMapping("/api/examens/{examenId}/periode")
    @PreAuthorize(DroitsExamen.GESTION)
    public ExamenDto periode(@PathVariable Long examenId,
                             @RequestBody com.plateforme.scolaire.web.dto.PeriodeExamenRequest r) {
        return examenService.definirPeriode(examenId, r);
    }

    @DeleteMapping("/api/examens/{examenId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(DroitsExamen.GESTION)
    public void supprimer(@PathVariable Long examenId, @RequestParam(defaultValue = "false") boolean forcer) {
        examenService.supprimer(examenId, forcer);
    }
}
