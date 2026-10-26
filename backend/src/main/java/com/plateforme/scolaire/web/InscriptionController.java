package com.plateforme.scolaire.web;

import com.plateforme.scolaire.service.InscriptionService;
import com.plateforme.scolaire.web.dto.DecisionRequest;
import com.plateforme.scolaire.web.dto.InscriptionDto;
import com.plateforme.scolaire.web.dto.InscriptionRequest;
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

@RestController
@RequiredArgsConstructor
public class InscriptionController {

    private final InscriptionService inscriptionService;

    @GetMapping("/api/etablissements/{etablissementId}/inscriptions")
    @PreAuthorize("@perimetre.peutAccederEtablissement(#etablissementId)")
    public List<InscriptionDto> lister(@PathVariable Long etablissementId,
                                       @RequestParam(required = false) Long anneeScolaireId,
                                       @RequestParam(required = false) String statut) {
        return inscriptionService.lister(etablissementId, anneeScolaireId, statut);
    }

    @PostMapping("/api/etablissements/{etablissementId}/inscriptions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@perimetre.peutGererEtablissement(#etablissementId)")
    public InscriptionDto demander(@PathVariable Long etablissementId,
                                   @Valid @RequestBody InscriptionRequest requete) {
        return inscriptionService.demander(etablissementId, requete);
    }

    @GetMapping("/api/inscriptions/{id}")
    @PreAuthorize("@perimetre.peutAccederInscription(#id)")
    public InscriptionDto detail(@PathVariable Long id) {
        return inscriptionService.trouver(id);
    }

    @PostMapping("/api/inscriptions/{id}/decision")
    @PreAuthorize("@perimetre.peutGererInscription(#id)")
    public InscriptionDto decider(@PathVariable Long id, @Valid @RequestBody DecisionRequest requete) {
        return inscriptionService.decider(id, requete);
    }

    @PostMapping("/api/inscriptions/{id}/annulation")
    @PreAuthorize("@perimetre.peutGererInscription(#id)")
    public InscriptionDto annuler(@PathVariable Long id) {
        return inscriptionService.annuler(id);
    }
}
