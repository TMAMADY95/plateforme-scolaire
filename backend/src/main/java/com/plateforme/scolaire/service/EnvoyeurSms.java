package com.plateforme.scolaire.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Envoi de SMS. Sans passerelle configurée (aucun bean {@link PasserelleSms}), rien n'est envoyé : en développement
 * le message peut être écrit dans les journaux (app.sms.journaliser=true). Le numéro n'est jamais journalisé en entier.
 */
@Slf4j
@Component
public class EnvoyeurSms {

    static final int LONGUEUR_MAX = 160;

    private final ObjectProvider<PasserelleSms> passerelle;
    private final boolean journaliser;

    public EnvoyeurSms(ObjectProvider<PasserelleSms> passerelle,
                       @Value("${app.sms.journaliser:false}") boolean journaliser) {
        this.passerelle = passerelle;
        this.journaliser = journaliser;
    }

    /** @return true si le SMS a été remis à la passerelle */
    public boolean envoyer(String numero, String texte) {
        String n = NumeroTelephone.normaliser(numero);
        if (n == null) {
            log.warn("SMS non envoyé : numéro inutilisable");
            return false;
        }
        String court = texte.length() <= LONGUEUR_MAX ? texte : texte.substring(0, LONGUEUR_MAX - 1) + "…";
        PasserelleSms p = passerelle.getIfAvailable();
        if (p == null) {
            if (journaliser) {
                log.info("[SMS NON ENVOYÉ : aucune passerelle] à {} — {}", masque(n), court);
            } else {
                log.warn("Aucune passerelle SMS configurée : SMS non envoyé à {}", masque(n));
            }
            return false;
        }
        p.envoyer(n, court);
        return true;
    }

    private static String masque(String n) {
        return n.length() <= 4 ? "****" : "****" + n.substring(n.length() - 3);
    }
}
