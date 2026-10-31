package com.plateforme.scolaire.web;

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
    @PreAuthorize("hasAnyRole('MINISTERE','DIRECTION_REGIONALE')")
    public List<com.plateforme.scolaire.web.dto.TypeExamenDto> types() {
        return typesExamen.lister();
    }

    @PostMapping("/api/types-examen")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('MINISTERE')")
    public com.plateforme.scolaire.web.dto.TypeExamenDto creerType(
            @Valid @RequestBody com.plateforme.scolaire.web.dto.TypeExamenRequest r) {
        return typesExamen.creer(r);
    }

    @PutMapping("/api/types-examen/{id}")
    @PreAuthorize("hasRole('MINISTERE')")
    public com.plateforme.scolaire.web.dto.TypeExamenDto modifierType(
            @PathVariable Long id, @Valid @RequestBody com.plateforme.scolaire.web.dto.TypeExamenRequest r) {
        return typesExamen.modifier(id, r);
    }

    @DeleteMapping("/api/types-examen/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('MINISTERE')")
    public void supprimerType(@PathVariable Long id) {
        typesExamen.supprimer(id);
    }

    /** NATIONAL et les types de zone du référentiel : les niveaux qui peuvent organiser un examen. */
    @GetMapping("/api/examens/niveaux-organisation")
    @PreAuthorize("hasAnyRole('MINISTERE','DIRECTION_REGIONALE')")
    public List<String> niveauxOrganisation() {
        return examenService.niveauxOrganisation();
    }

    @GetMapping("/api/examens")
    @PreAuthorize("hasAnyRole('MINISTERE','DIRECTION_REGIONALE')")
    public List<ExamenDto> lister(@RequestParam Long anneeScolaireId) {
        return examenService.lister(anneeScolaireId);
    }

    @PostMapping("/api/examens")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('MINISTERE')")
    public ExamenDto creer(@Valid @RequestBody ExamenRequest r) {
        return examenService.creer(r);
    }

    @GetMapping("/api/ministere/examens/{examenId}")
    @PreAuthorize("hasRole('MINISTERE')")
    public RapportExamenDto nationale(@PathVariable Long examenId, @RequestParam(required = false) Long zoneId) {
        return examenService.nationale(examenId, zoneId);
    }

    @GetMapping("/api/region/examens/{examenId}")
    @PreAuthorize("hasRole('DIRECTION_REGIONALE')")
    public RapportExamenDto regionale(@PathVariable Long examenId) {
        return examenService.regionale(examenId);
    }

    @PutMapping("/api/examens/{examenId}/resultats/{etablissementId}")
    @PreAuthorize("hasAnyRole('MINISTERE','DIRECTION_REGIONALE') and @perimetre.peutAccederEtablissement(#etablissementId)")
    public void saisir(@PathVariable Long examenId, @PathVariable Long etablissementId,
                       @Valid @RequestBody ResultatExamenRequest r) {
        examenService.saisir(examenId, etablissementId, r);
    }

    @PutMapping("/api/examens/{examenId}/periode")
    @PreAuthorize("hasAnyRole('MINISTERE','DIRECTION_REGIONALE')")
    public ExamenDto periode(@PathVariable Long examenId,
                             @RequestBody com.plateforme.scolaire.web.dto.PeriodeExamenRequest r) {
        return examenService.definirPeriode(examenId, r);
    }

    @DeleteMapping("/api/examens/{examenId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('MINISTERE','DIRECTION_REGIONALE')")
    public void supprimer(@PathVariable Long examenId, @RequestParam(defaultValue = "false") boolean forcer) {
        examenService.supprimer(examenId, forcer);
    }
}
