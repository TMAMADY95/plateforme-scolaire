package com.plateforme.scolaire.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Envoi d'e-mails texte. Sans serveur SMTP configuré (variable SPRING_MAIL_HOST), rien n'est envoyé : en
 * développement le message est écrit dans les journaux (app.mail.journaliser=true) pour pouvoir suivre le lien.
 * En production le contenu n'est jamais journalisé : il contient des liens secrets.
 */
@Slf4j
@Component
public class EnvoyeurMail {

    private final ObjectProvider<JavaMailSender> expediteur;
    private final String de;
    private final boolean journaliser;

    public EnvoyeurMail(ObjectProvider<JavaMailSender> expediteur,
                        @Value("${app.mail.de:no-reply@plateforme-scolaire.local}") String de,
                        @Value("${app.mail.journaliser:false}") boolean journaliser) {
        this.expediteur = expediteur;
        this.de = de;
        this.journaliser = journaliser;
    }

    public void envoyer(String destinataire, String sujet, String texte) {
        JavaMailSender envoi = expediteur.getIfAvailable();
        if (envoi == null) {
            if (journaliser) {
                log.info("[E-MAIL NON ENVOYÉ : SMTP non configuré] à {} — {}\n{}", destinataire, sujet, texte);
            } else {
                log.warn("SMTP non configuré : e-mail « {} » non envoyé à {}", sujet, destinataire);
            }
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(de);
        message.setTo(destinataire);
        message.setSubject(sujet);
        message.setText(texte);
        envoi.send(message);
    }
}
