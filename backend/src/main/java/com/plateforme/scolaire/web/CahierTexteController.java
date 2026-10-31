package com.plateforme.scolaire.web;

import com.plateforme.scolaire.service.CahierTexteService;
import com.plateforme.scolaire.web.dto.CahierTexteDto;
import com.plateforme.scolaire.web.dto.CahierTexteRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class CahierTexteController {

    private final CahierTexteService service;

    @GetMapping("/api/classes/{classeId}/cahier-texte")
    @PreAuthorize("@perimetreEmploiDuTemps.peutConsulterClasse(#classeId)")
    public List<CahierTexteDto> lister(@PathVariable Long classeId,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au) {
        return service.lister(classeId, du, au);
    }

    @GetMapping("/api/eleves/{id}/cahier-texte")
    @PreAuthorize("@perimetre.peutAccederEleve(#id)")
    public List<CahierTexteDto> deLEleve(@PathVariable Long id) {
        return service.deLEleve(id);
    }

    @PostMapping("/api/classes/{classeId}/cahier-texte")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@perimetreNotes.peutCreerEvaluation(#classeId)")
    public CahierTexteDto creer(@PathVariable Long classeId, @Valid @RequestBody CahierTexteRequest r) {
        return service.creer(classeId, r);
    }

    @DeleteMapping("/api/cahier-texte/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("isAuthenticated()")
    public void supprimer(@PathVariable Long id) {
        service.supprimer(id);
    }
}
