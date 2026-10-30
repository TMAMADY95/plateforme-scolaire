package com.plateforme.scolaire.web;

import com.plateforme.scolaire.security.UtilisateurPrincipal;
import com.plateforme.scolaire.service.ParentService;
import com.plateforme.scolaire.service.ResumeParentService;
import com.plateforme.scolaire.web.dto.EnfantDto;
import com.plateforme.scolaire.web.dto.LienParentalDto;
import com.plateforme.scolaire.web.dto.ParentRequest;
import com.plateforme.scolaire.web.dto.ResumeEnfantDto;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ParentController {

    private final ParentService parentService;
    private final ResumeParentService resumeParentService;

    @PostMapping("/api/eleves/{id}/parents")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@perimetre.peutGererEleve(#id)")
    public LienParentalDto lier(@PathVariable Long id, @Valid @RequestBody ParentRequest requete) {
        return parentService.lier(id, requete);
    }

    /** Les enfants du parent connecté. */
    @GetMapping("/api/me/enfants")
    @PreAuthorize("hasRole('PARENT')")
    public List<EnfantDto> mesEnfants(@AuthenticationPrincipal UtilisateurPrincipal principal) {
        return parentService.mesEnfants(principal.getId());
    }

    /** Le point du jour sur chaque enfant du parent connecté. */
    @GetMapping("/api/me/resume")
    @PreAuthorize("hasRole('PARENT')")
    public List<ResumeEnfantDto> resume(@AuthenticationPrincipal UtilisateurPrincipal principal) {
        return resumeParentService.resume(principal.getId());
    }

    /** Les parents d'un élève, pour la direction qui le gère. */
    @GetMapping("/api/eleves/{id}/parents")
    @PreAuthorize("@perimetre.peutGererEleve(#id)")
    public List<LienParentalDto> parents(@PathVariable Long id) {
        return parentService.parentsDeLEleve(id);
    }
}
