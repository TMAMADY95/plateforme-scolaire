package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.LienParental;
import com.plateforme.scolaire.domain.Presence;
import com.plateforme.scolaire.repository.AlerteEnvoyeeRepository;
import com.plateforme.scolaire.repository.LienParentalRepository;
import com.plateforme.scolaire.repository.PresenceRepository;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Prévient par SMS les parents d'un élève noté absent (non justifié) à l'appel. Un SMS réussi n'est jamais
 * renvoyé pour la même absence, même si l'appel est corrigé ensuite. Un échec d'envoi ne bloque jamais l'appel.
 */
@Slf4j
@Service
public class AbsencesParentsService {

    static final String TYPE = "ABSENCE";
    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final PresenceRepository presences;
    private final LienParentalRepository liens;
    private final AlerteEnvoyeeRepository alertes;
    private final EnvoyeurSms sms;
    private final JournalAlertes journal;
    private final boolean actif;

    public AbsencesParentsService(PresenceRepository presences, LienParentalRepository liens, AlerteEnvoyeeRepository alertes,
                                  EnvoyeurSms sms, JournalAlertes journal, @Value("${app.sms.absences:false}") boolean actif) {
        this.presences = presences;
        this.liens = liens;
        this.alertes = alertes;
        this.sms = sms;
        this.journal = journal;
        this.actif = actif;
    }

    @Transactional
    public int prevenir(Long classeId, LocalDate date) {
        if (!actif) {
            return 0;
        }
        int envoyes = 0;
        try {
            for (Presence p : presences.findByInscriptionClasseIdAndDateSeance(classeId, date)) {
                if ("ABSENT".equals(p.getStatut()) && !p.isJustifie()) {
                    envoyes += prevenirPour(p, date);
                }
            }
        } catch (RuntimeException e) {
            log.warn("Notification d'absence interrompue : {}", e.getMessage());
        }
        return envoyes;
    }

    private int prevenirPour(Presence p, LocalDate date) {
        String cle = "ABS#" + p.getId() + " ";
        if (alertes.existsByTypeAndResumeStartingWithAndReussieTrue(TYPE, cle)) {
            return 0;
        }
        var eleve = p.getInscription().getEleve();
        String texte = eleve.getPrenom() + " " + eleve.getNom() + " était absent(e) le " + date.format(JOUR)
                + ". Contactez l'établissement en cas de besoin.";
        List<LienParental> parents = liens.findByEleveId(eleve.getId());
        int envoyes = 0;
        for (LienParental lien : parents) {
            var parent = lien.getUtilisateur();
            if (!parent.isActif() || parent.getTelephone() == null || parent.getTelephone().isBlank()) {
                continue;
            }
            boolean ok = sms.envoyer(parent.getTelephone(), texte);
            journal.noter(TYPE, "SMS", parent.getEmail(), cle + texte, ok);
            if (ok) {
                envoyes++;
            }
        }
        return envoyes;
    }
}
