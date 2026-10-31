package com.plateforme.scolaire.service;

/**
 * Point de branchement d'un fournisseur de SMS (Orange, Twilio, passerelle locale…). Déclarer un bean qui
 * implémente cette interface suffit : {@link EnvoyeurSms} l'utilise alors à la place de la journalisation.
 */
public interface PasserelleSms {

    /** @param numeroE164 numéro au format international, par exemple +224620000000 */
    void envoyer(String numeroE164, String texte);
}
