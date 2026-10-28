package com.plateforme.scolaire.web;

import com.plateforme.scolaire.service.ComptesService;
import com.plateforme.scolaire.web.dto.CompteDto;
import com.plateforme.scolaire.web.dto.ComptePersonnelDto;
import com.plateforme.scolaire.web.dto.PersonnelRequest;
import com.plateforme.scolaire.web.dto.ReinitialisationRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Administration des comptes par le ministère (directions) et par les directions d'établissement. */
@RestController
@RequiredArgsConstructor
public class ComptesController {

    private final ComptesService comptesService;

    @GetMapping("/api/etablissements/{etablissementId}/comptes")
    @PreAuthorize("@perimetreComptes.peutListerComptes(#etablissementId)")
    public List<CompteDto> lister(@PathVariable Long etablissementId,
                                  @RequestParam(defaultValue = "") String q) {
        return comptesService.lister(etablissementId, q);
    }

    @PostMapping("/api/etablissements/{etablissementId}/personnel")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@perimetreComptes.estDirectionDe(#etablissementId)")
    public ComptePersonnelDto creerPersonnel(@PathVariable Long etablissementId,
                                             @Valid @RequestBody PersonnelRequest requete) {
        return comptesService.creerPersonnel(etablissementId, requete);
    }

    @PostMapping("/api/utilisateurs/{id}/reinitialisation-mot-de-passe")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@perimetreComptes.peutReinitialiser(#id)")
    public void reinitialiser(@PathVariable Long id, @Valid @RequestBody ReinitialisationRequest requete) {
        comptesService.reinitialiserMotDePasse(id, requete.motDePasseTemporaire());
    }
}
