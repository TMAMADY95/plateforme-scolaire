package com.plateforme.scolaire.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Faux prestataire pour développer et tester le parcours complet sans compte marchand.
 * Actif seulement si {@code app.paiement.fournisseur=simule} (profil dev) : jamais en production.
 */
@Component
@ConditionalOnProperty(name = "app.paiement.fournisseur", havingValue = "simule")
public class PasserelleSimulee implements PasserelleDePaiement {

    private final String urlPublique;

    public PasserelleSimulee(@Value("${app.paiement.url-publique:http://localhost:5173}") String urlPublique) {
        this.urlPublique = urlPublique;
    }

    @Override
    public ResultatInitiation initier(DemandePaiement demande) {
        return new ResultatInitiation("SIM-" + demande.reference(), urlPublique + "/paiement-simule/" + demande.reference());
    }
}
