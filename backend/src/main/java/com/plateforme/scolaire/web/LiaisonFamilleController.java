package com.plateforme.scolaire.web;

import com.plateforme.scolaire.service.AutorisationSortieService;
import com.plateforme.scolaire.service.RendezVousService;
import com.plateforme.scolaire.web.dto.EnseignantDeLEleveDto;
import com.plateforme.scolaire.web.dto.ReponseRequest;
import com.plateforme.scolaire.web.dto.RendezVousDto;
import com.plateforme.scolaire.web.dto.RendezVousRequest;
import com.plateforme.scolaire.web.dto.SortieDto;
import com.plateforme.scolaire.web.dto.SortieRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Liaison école-famille : rendez-vous avec les enseignants et autorisations de sortie. */
@RestController
@RequiredArgsConstructor
public class LiaisonFamilleController {

    private final RendezVousService rendezVous;
    private final AutorisationSortieService sorties;

    @GetMapping("/api/eleves/{id}/enseignants")
    @PreAuthorize("@perimetre.peutAccederEleve(#id)")
    public List<EnseignantDeLEleveDto> enseignants(@PathVariable Long id) {
        return rendezVous.enseignantsDe(id);
    }

    @GetMapping("/api/eleves/{id}/rendez-vous")
    @PreAuthorize("@perimetre.peutAccederEleve(#id)")
    public List<RendezVousDto> rendezVous(@PathVariable Long id) {
        return rendezVous.deLEleve(id);
    }

    @PostMapping("/api/eleves/{id}/rendez-vous")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@perimetre.peutAccederEleve(#id)")
    public RendezVousDto demander(@PathVariable Long id, @Valid @RequestBody RendezVousRequest r) {
        return rendezVous.demander(id, r);
    }

    @PostMapping("/api/rendez-vous/{id}/annulation")
    @PreAuthorize("isAuthenticated()")
    public RendezVousDto annuler(@PathVariable Long id) {
        return rendezVous.annuler(id);
    }

    @GetMapping("/api/me/rendez-vous")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    public List<RendezVousDto> mesRendezVous() {
        return rendezVous.demandesDeLEnseignant();
    }

    @PostMapping("/api/rendez-vous/{id}/reponse")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    public RendezVousDto repondre(@PathVariable Long id, @Valid @RequestBody ReponseRequest r) {
        return rendezVous.repondre(id, r.accepter(), r.reponse());
    }

    @GetMapping("/api/eleves/{id}/sorties")
    @PreAuthorize("@perimetre.peutAccederEleve(#id)")
    public List<SortieDto> sortiesDeLEleve(@PathVariable Long id) {
        return sorties.deLEleve(id);
    }

    @PostMapping("/api/eleves/{id}/sorties")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@perimetre.peutAccederEleve(#id)")
    public SortieDto demanderSortie(@PathVariable Long id, @Valid @RequestBody SortieRequest r) {
        return sorties.demander(id, r);
    }

    @GetMapping("/api/etablissements/{id}/sorties")
    @PreAuthorize("@perimetre.peutGererEtablissement(#id)")
    public List<SortieDto> sortiesDeLEtablissement(@PathVariable Long id) {
        return sorties.deLEtablissement(id);
    }

    @PostMapping("/api/sorties/{id}/decision")
    @PreAuthorize("isAuthenticated()")
    public SortieDto decider(@PathVariable Long id, @Valid @RequestBody ReponseRequest r) {
        return sorties.decider(id, r);
    }
}
