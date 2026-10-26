package com.plateforme.scolaire.web;

import com.plateforme.scolaire.service.EnseignantService;
import com.plateforme.scolaire.web.dto.AffectationEnseignantDto;
import com.plateforme.scolaire.web.dto.AffectationEnseignantRequest;
import com.plateforme.scolaire.web.dto.EnseignantDto;
import com.plateforme.scolaire.web.dto.EnseignantRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class EnseignantController {

    private final EnseignantService enseignantService;

    @GetMapping("/api/etablissements/{etablissementId}/enseignants")
    @PreAuthorize("@perimetre.peutAccederEtablissement(#etablissementId)")
    public List<EnseignantDto> lister(@PathVariable Long etablissementId) {
        return enseignantService.lister(etablissementId);
    }

    @PostMapping("/api/etablissements/{etablissementId}/enseignants")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@perimetre.peutGererEtablissement(#etablissementId)")
    public EnseignantDto creer(@PathVariable Long etablissementId, @Valid @RequestBody EnseignantRequest requete) {
        return enseignantService.creer(etablissementId, requete);
    }

    @GetMapping("/api/classes/{id}/enseignants")
    @PreAuthorize("@perimetre.peutAccederClasse(#id)")
    public List<AffectationEnseignantDto> affectations(@PathVariable Long id) {
        return enseignantService.affectations(id);
    }

    @PostMapping("/api/classes/{id}/enseignants")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@perimetre.peutGererClasse(#id)")
    public AffectationEnseignantDto affecter(@PathVariable Long id,
                                             @Valid @RequestBody AffectationEnseignantRequest requete) {
        return enseignantService.affecter(id, requete);
    }
}
