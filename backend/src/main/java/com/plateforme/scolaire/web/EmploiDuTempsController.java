package com.plateforme.scolaire.web;

import com.plateforme.scolaire.service.EmploiDuTempsService;
import com.plateforme.scolaire.web.dto.CreneauDto;
import com.plateforme.scolaire.web.dto.CreneauRequest;
import com.plateforme.scolaire.web.dto.SalleDto;
import com.plateforme.scolaire.web.dto.SalleRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Salles et emplois du temps (Module 9). */
@RestController
@RequiredArgsConstructor
public class EmploiDuTempsController {

    private final EmploiDuTempsService emploiDuTempsService;

    // ---------- Salles ----------

    @GetMapping("/api/etablissements/{etablissementId}/salles")
    @PreAuthorize("@perimetre.peutAccederEtablissement(#etablissementId)")
    public List<SalleDto> listerSalles(@PathVariable Long etablissementId) {
        return emploiDuTempsService.listerSalles(etablissementId);
    }

    @PostMapping("/api/etablissements/{etablissementId}/salles")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@perimetre.peutGererEtablissement(#etablissementId)")
    public SalleDto creerSalle(@PathVariable Long etablissementId, @Valid @RequestBody SalleRequest requete) {
        return emploiDuTempsService.creerSalle(etablissementId, requete);
    }

    @DeleteMapping("/api/etablissements/{etablissementId}/salles/{salleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@perimetre.peutGererEtablissement(#etablissementId)")
    public void supprimerSalle(@PathVariable Long etablissementId, @PathVariable Long salleId) {
        emploiDuTempsService.supprimerSalle(etablissementId, salleId);
    }

    // ---------- Créneaux ----------

    @GetMapping("/api/classes/{classeId}/creneaux")
    @PreAuthorize("@perimetre.peutConsulterPresences(#classeId)")
    public List<CreneauDto> listerCreneauxClasse(@PathVariable Long classeId) {
        return emploiDuTempsService.listerCreneauxClasse(classeId);
    }

    @GetMapping("/api/etablissements/{etablissementId}/creneaux")
    @PreAuthorize("@perimetre.peutAccederEtablissement(#etablissementId)")
    public List<CreneauDto> listerCreneauxEtablissement(@PathVariable Long etablissementId,
                                                        @RequestParam Long anneeScolaireId) {
        return emploiDuTempsService.listerCreneauxEtablissement(etablissementId, anneeScolaireId);
    }

    @PostMapping("/api/classes/{classeId}/creneaux")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@perimetre.peutGererClasse(#classeId)")
    public CreneauDto creerCreneau(@PathVariable Long classeId, @Valid @RequestBody CreneauRequest requete) {
        return emploiDuTempsService.creerCreneau(classeId, requete);
    }

    @PutMapping("/api/creneaux/{id}")
    @PreAuthorize("@perimetreEmploiDuTemps.peutGererCreneau(#id)")
    public CreneauDto modifierCreneau(@PathVariable Long id, @Valid @RequestBody CreneauRequest requete) {
        return emploiDuTempsService.modifierCreneau(id, requete);
    }

    @DeleteMapping("/api/creneaux/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@perimetreEmploiDuTemps.peutGererCreneau(#id)")
    public void supprimerCreneau(@PathVariable Long id) {
        emploiDuTempsService.supprimerCreneau(id);
    }
}
