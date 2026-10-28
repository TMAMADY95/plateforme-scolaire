package com.plateforme.scolaire.dev;

import com.plateforme.scolaire.domain.Paiement;
import com.plateforme.scolaire.repository.PaiementRepository;
import com.plateforme.scolaire.service.PaiementService;
import com.plateforme.scolaire.web.dto.NotificationPaiement;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Joue le rôle du prestataire pour le faux paiement (profil dev uniquement, n'existe pas en production).
 * Il fabrique la même notification signée qu'un vrai prestataire enverrait.
 */
@RestController
@Profile("dev")
@RequiredArgsConstructor
public class DevPaiementController {

    private final PaiementRepository paiementRepository;
    private final PaiementService paiementService;

    @Value("${app.paiement.secret-webhook:}")
    private String secret;

    @PostMapping("/api/dev/paiements/{reference}/simuler")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void simuler(@PathVariable String reference, @RequestParam boolean succes) {
        Paiement paiement = paiementRepository.findByReferenceInterne(reference)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Paiement introuvable"));
        String statut = succes ? "SUCCES" : "ECHEC";
        String signature = PaiementService.signer(secret, reference, statut, paiement.getMontant(),
                paiement.getReferenceExterne());
        paiementService.traiterNotification(new NotificationPaiement(
                reference, statut, paiement.getMontant(), paiement.getReferenceExterne(), signature));
    }
}
