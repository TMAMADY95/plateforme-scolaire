package com.plateforme.scolaire.web;

import com.plateforme.scolaire.security.PerimetreService;
import com.plateforme.scolaire.service.PaiementService;
import com.plateforme.scolaire.web.dto.AnnulationPaiementRequest;
import com.plateforme.scolaire.web.dto.InitierPaiementRequest;
import com.plateforme.scolaire.web.dto.NotificationPaiement;
import com.plateforme.scolaire.web.dto.PaiementDto;
import com.plateforme.scolaire.web.dto.PaiementInitieDto;
import com.plateforme.scolaire.web.dto.PaiementRequest;
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

/** Paiements des factures (Étape 3b, Module 7). */
@RestController
@RequiredArgsConstructor
public class PaiementController {

    private final PaiementService paiementService;
    private final PerimetreService perimetreService;

    /** Historique des paiements d'une facture : direction et personnel administratif. */
    @GetMapping("/api/factures/{id}/paiements")
    @PreAuthorize("@perimetreFinances.peutAccederFacture(#id)")
    public List<PaiementDto> lister(@PathVariable Long id) {
        return paiementService.lister(id);
    }

    /** Encaissement hors ligne saisi par le personnel. */
    @PostMapping("/api/factures/{id}/paiements")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@perimetreFinances.peutGererFacture(#id)")
    public PaiementDto enregistrer(@PathVariable Long id, @Valid @RequestBody PaiementRequest requete) {
        return paiementService.enregistrerHorsLigne(id, requete);
    }

    /** Annulation d'un encaissement hors ligne (erreur de saisie...) par la direction ou l'administration. */
    @PostMapping("/api/factures/{factureId}/paiements/{paiementId}/annulation")
    @PreAuthorize("@perimetreFinances.peutGererFacture(#factureId)")
    public PaiementDto annuler(@PathVariable Long factureId, @PathVariable Long paiementId,
                               @Valid @RequestBody AnnulationPaiementRequest requete) {
        return paiementService.annulerHorsLigne(factureId, paiementId, requete.motif());
    }

    /** Paiement en ligne d'une de SES factures (élève) ou de celles de ses enfants (parent). */
    @PostMapping("/api/me/factures/{id}/paiements")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ELEVE', 'PARENT')")
    public PaiementInitieDto payerEnLigne(@PathVariable Long id,
                                          @Valid @RequestBody(required = false) InitierPaiementRequest requete) {
        return paiementService.initierEnLigne(perimetreService.utilisateurCourant().getId(), id,
                requete == null ? null : requete.montant());
    }

    /** Appelé par le prestataire (sans jeton) : la sécurité repose sur la signature de la notification. */
    @PostMapping("/api/paiements/notification")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void notification(@Valid @RequestBody NotificationPaiement notification) {
        paiementService.traiterNotification(notification);
    }
}
