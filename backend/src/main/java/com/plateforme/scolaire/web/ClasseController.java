package com.plateforme.scolaire.web;

import com.plateforme.scolaire.service.ClasseService;
import com.plateforme.scolaire.web.dto.ClasseDto;
import com.plateforme.scolaire.web.dto.ClasseModification;
import com.plateforme.scolaire.web.dto.ClasseRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ClasseController {

    private final ClasseService classeService;

    @GetMapping("/api/etablissements/{etablissementId}/classes")
    @PreAuthorize("@perimetre.peutAccederEtablissement(#etablissementId)")
    public List<ClasseDto> lister(@PathVariable Long etablissementId,
                                  @RequestParam(required = false) Long anneeScolaireId) {
        return classeService.lister(etablissementId, anneeScolaireId);
    }

    @PostMapping("/api/etablissements/{etablissementId}/classes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@perimetre.peutGererEtablissement(#etablissementId)")
    public ClasseDto creer(@PathVariable Long etablissementId, @Valid @RequestBody ClasseRequest requete) {
        return classeService.creer(etablissementId, requete);
    }

    @GetMapping("/api/classes/{id}")
    @PreAuthorize("@perimetre.peutAccederClasse(#id)")
    public ClasseDto detail(@PathVariable Long id) {
        return classeService.trouver(id);
    }

    @PutMapping("/api/classes/{id}")
    @PreAuthorize("@perimetre.peutGererClasse(#id)")
    public ClasseDto modifier(@PathVariable Long id, @Valid @RequestBody ClasseModification requete) {
        return classeService.modifier(id, requete);
    }
}
