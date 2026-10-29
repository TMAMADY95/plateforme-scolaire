package com.plateforme.scolaire.web;

import com.plateforme.scolaire.service.CoefficientMatiereService;
import com.plateforme.scolaire.web.dto.CoefficientMatiereDto;
import com.plateforme.scolaire.web.dto.CoefficientsRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CoefficientMatiereController {

    private final CoefficientMatiereService service;

    @GetMapping("/api/classes/{classeId}/coefficients")
    @PreAuthorize("@perimetreNotes.peutConsulterClasse(#classeId)")
    public List<CoefficientMatiereDto> lister(@PathVariable Long classeId) {
        return service.lister(classeId);
    }

    /** Réservé à la gestion de l'établissement : les coefficients changent les moyennes de toute la classe. */
    @PutMapping("/api/classes/{classeId}/coefficients")
    @PreAuthorize("@perimetre.peutGererClasse(#classeId)")
    public List<CoefficientMatiereDto> enregistrer(@PathVariable Long classeId,
                                                   @Valid @RequestBody CoefficientsRequest requete) {
        return service.enregistrer(classeId, requete);
    }
}
