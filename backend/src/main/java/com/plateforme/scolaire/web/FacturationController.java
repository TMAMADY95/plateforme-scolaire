package com.plateforme.scolaire.web;

import com.plateforme.scolaire.security.PerimetreService;
import com.plateforme.scolaire.security.UtilisateurPrincipal;
import com.plateforme.scolaire.service.FacturationService;
import com.plateforme.scolaire.service.FraisService;
import com.plateforme.scolaire.service.MesFacturesService;
import com.plateforme.scolaire.web.dto.BilanFinancierDto;
import com.plateforme.scolaire.web.dto.ExonerationRequest;
import com.plateforme.scolaire.web.dto.FactureDto;
import com.plateforme.scolaire.web.dto.FraisDto;
import com.plateforme.scolaire.web.dto.FraisRequest;
import com.plateforme.scolaire.web.dto.GenererFacturesRequest;
import com.plateforme.scolaire.web.dto.ResultatGenerationDto;
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

/** Frais scolaires, factures et soldes (Étape 3a, Module 7). */
@RestController
@RequiredArgsConstructor
public class FacturationController {

    private final FraisService fraisService;
    private final FacturationService facturationService;
    private final MesFacturesService mesFacturesService;
    private final PerimetreService perimetreService;

    // ---------- Grille tarifaire ----------

    @GetMapping("/api/etablissements/{etablissementId}/frais")
    @PreAuthorize("@perimetre.peutAccederEtablissement(#etablissementId)")
    public List<FraisDto> listerFrais(@PathVariable Long etablissementId,
                                      @RequestParam(required = false) Long anneeScolaireId) {
        return fraisService.lister(etablissementId, anneeScolaireId);
    }

    @GetMapping("/api/frais/{id}")
    @PreAuthorize("@perimetreFinances.peutAccederFrais(#id)")
    public FraisDto detailFrais(@PathVariable Long id) {
        return fraisService.trouver(id);
    }

    @PostMapping("/api/etablissements/{etablissementId}/frais")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@perimetre.peutGererEtablissement(#etablissementId)")
    public FraisDto creerFrais(@PathVariable Long etablissementId, @Valid @RequestBody FraisRequest requete) {
        return fraisService.creer(etablissementId, requete);
    }

    @PutMapping("/api/frais/{id}")
    @PreAuthorize("@perimetreFinances.peutGererFrais(#id)")
    public FraisDto modifierFrais(@PathVariable Long id, @Valid @RequestBody FraisRequest requete) {
        return fraisService.modifier(id, requete);
    }

    @PostMapping("/api/frais/{id}/desactivation")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@perimetreFinances.peutGererFrais(#id)")
    public void desactiverFrais(@PathVariable Long id) {
        fraisService.desactiver(id);
    }

    // ---------- Factures ----------

    @GetMapping("/api/etablissements/{etablissementId}/factures")
    @PreAuthorize("@perimetre.peutAccederEtablissement(#etablissementId)")
    public List<FactureDto> listerFactures(@PathVariable Long etablissementId,
                                           @RequestParam Long anneeScolaireId,
                                           @RequestParam(required = false) String statut) {
        return facturationService.facturesDeLEtablissement(etablissementId, anneeScolaireId, statut);
    }

    @GetMapping("/api/etablissements/{etablissementId}/factures/en-retard")
    @PreAuthorize("@perimetre.peutAccederEtablissement(#etablissementId)")
    public List<FactureDto> facturesEnRetard(@PathVariable Long etablissementId) {
        return facturationService.facturesEnRetard(etablissementId);
    }

    @GetMapping("/api/etablissements/{etablissementId}/factures/bilan")
    @PreAuthorize("@perimetre.peutAccederEtablissement(#etablissementId)")
    public BilanFinancierDto bilan(@PathVariable Long etablissementId,
                                   @RequestParam Long anneeScolaireId) {
        return facturationService.bilan(etablissementId, anneeScolaireId);
    }

    @GetMapping("/api/factures/{id}")
    @PreAuthorize("@perimetreFinances.peutAccederFacture(#id)")
    public FactureDto detailFacture(@PathVariable Long id) {
        return facturationService.trouver(id);
    }

    @PostMapping("/api/etablissements/{etablissementId}/factures/generation")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@perimetre.peutGererEtablissement(#etablissementId)")
    public ResultatGenerationDto generer(@PathVariable Long etablissementId,
                                         @Valid @RequestBody GenererFacturesRequest requete) {
        return facturationService.generer(etablissementId, requete);
    }

    @PostMapping("/api/factures/{id}/exonerations")
    @PreAuthorize("@perimetreFinances.peutGererFacture(#id)")
    public FactureDto exonerer(@PathVariable Long id, @Valid @RequestBody ExonerationRequest requete) {
        return facturationService.exonerer(id, requete);
    }

    @PostMapping("/api/factures/{id}/annulation")
    @PreAuthorize("@perimetreFinances.peutGererFacture(#id)")
    public FactureDto annuler(@PathVariable Long id) {
        return facturationService.annuler(id);
    }

    // ---------- Espace élève et parent ----------
    // L'utilisateur est lu depuis le contexte de sécurité (voir PerimetreService),
    // plus fiable que l'injection par @AuthenticationPrincipal.

    /** Les factures de l'élève connecté. */
    @GetMapping("/api/me/factures")
    @PreAuthorize("hasRole('ELEVE')")
    public List<FactureDto> mesFactures() {
        UtilisateurPrincipal courant = perimetreService.utilisateurCourant();
        return mesFacturesService.facturesDeMonDossier(courant.getId());
    }

    /** Les factures d'un enfant du parent connecté. */
    @GetMapping("/api/me/enfants/{eleveId}/factures")
    @PreAuthorize("hasRole('PARENT')")
    public List<FactureDto> facturesDeMonEnfant(@PathVariable Long eleveId) {
        UtilisateurPrincipal courant = perimetreService.utilisateurCourant();
        return mesFacturesService.facturesDeMonEnfant(courant.getId(), eleveId);
    }
}
