package com.plateforme.scolaire.service;

import com.plateforme.scolaire.repository.LienParentalRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Prévient aussi par SMS les parents de la classe quand une annonce est publiée (si app.sms.annonces=true).
 * Le SMS ne contient que le titre. Beaucoup de parents ont un téléphone mais pas d'adresse e-mail.
 */
@Slf4j
@Component
public class NotificationAnnoncesSms {

    private final LienParentalRepository liens;
    private final EnvoyeurSms envoyeurSms;
    private final boolean actif;

    public NotificationAnnoncesSms(LienParentalRepository liens, EnvoyeurSms envoyeurSms,
                                   @Value("${app.sms.annonces:false}") boolean actif) {
        this.liens = liens;
        this.envoyeurSms = envoyeurSms;
        this.actif = actif;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void surAnnoncePubliee(AnnoncePubliee e) {
        notifier(e);
    }

    void notifier(AnnoncePubliee e) {
        if (!actif) {
            return;
        }
        try {
            String texte = "Nouvelle annonce, classe " + e.classeNom() + " : " + e.titre() + ". Connectez-vous pour la lire.";
            for (String numero : liens.telephonesParentsDeLaClasse(e.classeId())) {
                try {
                    envoyeurSms.envoyer(numero, texte);
                } catch (RuntimeException ex) {
                    log.warn("SMS d'annonce non envoyé (classe {})", e.classeId(), ex);
                }
            }
        } catch (RuntimeException ex) {
            log.warn("Notification SMS d'annonce impossible (classe {})", e.classeId(), ex);
        }
    }
}
