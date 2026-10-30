package com.plateforme.scolaire.service;

import com.plateforme.scolaire.repository.LienParentalRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Prévient par e-mail les parents de la classe quand une annonce est publiée. S'exécute après la validation de la
 * publication et hors de la requête : la publication n'attend pas l'envoi et ne peut pas échouer à cause de lui.
 * L'e-mail ne contient que le titre, jamais le texte de l'annonce.
 */
@Slf4j
@Component
public class NotificationAnnonces {

    private final LienParentalRepository liens;
    private final EnvoyeurMail envoyeurMail;
    private final boolean actif;
    private final String urlFront;

    public NotificationAnnonces(LienParentalRepository liens, EnvoyeurMail envoyeurMail,
                                @Value("${app.annonces.email:true}") boolean actif,
                                @Value("${app.mail.url-front:http://localhost:5173}") String urlFront) {
        this.liens = liens;
        this.envoyeurMail = envoyeurMail;
        this.actif = actif;
        this.urlFront = urlFront;
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
            for (String adresse : liens.emailsParentsDeLaClasse(e.classeId())) {
                try {
                    envoyeurMail.envoyer(adresse, "Nouvelle annonce – classe " + e.classeNom(),
                            "Bonjour,\n\n" + e.auteur() + " a publié une annonce pour la classe " + e.classeNom()
                                    + " : « " + e.titre() + " ».\n\nPour la lire, connectez-vous : " + urlFront + "\n");
                } catch (RuntimeException ex) {
                    // Une adresse en échec ne doit pas empêcher les suivantes.
                    log.warn("E-mail d'annonce non envoyé (classe {})", e.classeId(), ex);
                }
            }
        } catch (RuntimeException ex) {
            log.warn("Notification d'annonce impossible (classe {})", e.classeId(), ex);
        }
    }
}
