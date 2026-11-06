package com.plateforme.scolaire.web;

import com.plateforme.scolaire.security.DroitsExamen;
import com.plateforme.scolaire.service.ProfilEnseignantService;
import com.plateforme.scolaire.web.dto.ProfilEnseignantDto;
import jakarta.validation.Valid;
import java.security.Principal;
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
public class ProfilEnseignantController {

    private final ProfilEnseignantService service;

    /** Fiches nominatives : direction de l'établissement uniquement. */
    @GetMapping("/api/etablissements/{etablissementId}/enseignants-profils")
    @PreAuthorize("hasAnyRole('DIRECTION_ETABLISSEMENT','PERSONNEL_ADMIN') and @perimetre.peutAccederEtablissement(#etablissementId)")
    public List<ProfilEnseignantDto.Profil> lister(@PathVariable Long etablissementId) {
        return service.lister(etablissementId);
    }

    @PutMapping("/api/etablissements/{etablissementId}/enseignants/{enseignantId}/profil")
    @PreAuthorize("hasRole('DIRECTION_ETABLISSEMENT') and @perimetre.peutGererEtablissement(#etablissementId)")
    public ProfilEnseignantDto.Profil enregistrer(@PathVariable Long etablissementId, @PathVariable Long enseignantId,
            @Valid @RequestBody ProfilEnseignantDto.Requete requete, Principal principal) {
        return service.enregistrer(etablissementId, enseignantId, requete, principal == null ? null : principal.getName());
    }

    /** Totaux par établissement : ministère, DGECS, directions régionales et locales. */
    @GetMapping("/api/enseignants-synthese")
    @PreAuthorize(DroitsExamen.CONSULTATION)
    public ProfilEnseignantDto.Synthese synthese() {
        return service.synthese();
    }
}
