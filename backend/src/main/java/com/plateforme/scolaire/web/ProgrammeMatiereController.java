package com.plateforme.scolaire.web;

import com.plateforme.scolaire.service.ProgrammeMatiereService;
import com.plateforme.scolaire.web.dto.CoefficientMatiereDto;
import com.plateforme.scolaire.web.dto.CopieProgrammeRequest;
import com.plateforme.scolaire.web.dto.ProgrammeMatiereDto;
import com.plateforme.scolaire.web.dto.ProgrammeMatiereRequest;
import com.plateforme.scolaire.web.dto.ProgrammeResultatDto;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Programme officiel (matières, horaires, coefficients) : lecture pour tous, modification réservée au ministère. */
@RestController
@RequiredArgsConstructor
public class ProgrammeMatiereController {

    private static final String GESTION = "hasAnyRole('MINISTERE', 'ADMIN_TECHNIQUE')";

    private final ProgrammeMatiereService service;

    @GetMapping("/api/programmes")
    public List<ProgrammeMatiereDto> lister(@RequestParam Long anneeScolaireId, @RequestParam(required = false) Long niveauId) {
        return service.lister(anneeScolaireId, niveauId);
    }

    @PostMapping("/api/programmes")
    @PreAuthorize(GESTION)
    public ProgrammeMatiereDto creer(@Valid @RequestBody ProgrammeMatiereRequest requete) {
        return service.creer(requete);
    }

    @PutMapping("/api/programmes/{id}")
    @PreAuthorize(GESTION)
    public ProgrammeMatiereDto modifier(@PathVariable Long id, @Valid @RequestBody ProgrammeMatiereRequest requete) {
        return service.modifier(id, requete);
    }

    @DeleteMapping("/api/programmes/{id}")
    @PreAuthorize(GESTION)
    public void supprimer(@PathVariable Long id) {
        service.supprimer(id);
    }

    @PostMapping("/api/programmes/import-coc")
    @PreAuthorize(GESTION)
    public ProgrammeResultatDto importerCoc(@RequestParam Long anneeScolaireId) {
        return service.importerCoc(anneeScolaireId);
    }

    @PostMapping("/api/programmes/copie")
    @PreAuthorize(GESTION)
    public ProgrammeResultatDto copier(@Valid @RequestBody CopieProgrammeRequest requete) {
        return service.copier(requete);
    }

    /** Remplit les coefficients d'une classe à partir du programme de son niveau et de sa série. */
    @PostMapping("/api/classes/{classeId}/coefficients/depuis-programme")
    @PreAuthorize("@perimetre.peutGererClasse(#classeId)")
    public List<CoefficientMatiereDto> appliquer(@PathVariable Long classeId) {
        return service.appliquerAClasse(classeId);
    }
}
