package com.plateforme.scolaire.service;

import java.math.BigDecimal;

/**
 * Un prestataire de paiement en ligne (Mobile Money, carte...). Chaque pays et chaque opérateur a sa propre
 * API : on isole ici ce qui change. Le reste de l'application ne connaît que cette interface.
 *
 * <p>Une implémentation réelle doit : créer la transaction chez le prestataire, renvoyer l'adresse de paiement,
 * puis envoyer une notification signée à {@code POST /api/paiements/notification}.</p>
 */
public interface PasserelleDePaiement {

    ResultatInitiation initier(DemandePaiement demande);

    record DemandePaiement(String reference, BigDecimal montant, String devise, String libelle) {
    }

    record ResultatInitiation(String referenceExterne, String urlPaiement) {
    }
}
