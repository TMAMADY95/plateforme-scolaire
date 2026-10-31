package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.AffectationRole;
import com.plateforme.scolaire.domain.CandidatExamen;
import com.plateforme.scolaire.repository.AffectationRoleRepository;
import com.plateforme.scolaire.repository.AlerteEnvoyeeRepository;
import com.plateforme.scolaire.repository.CandidatExamenRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Rappelle à la direction d'un établissement qu'elle n'a transmis aucun résultat pour un examen dont les épreuves ont
 * commencé il y a plus de 14 jours alors qu'elle avait des candidats. Au plus un rappel par semaine et par examen.
 */
@Slf4j
@Service
@Transactional
public class RappelsResultatsExamenService {

    static final int JOURS_APRES_DEBUT = 14;
    static final Duration ENTRE_DEUX_RAPPELS = Duration.ofDays(7);
    static final String TYPE = "RESULTATS_EXAMEN";

    private final CandidatExamenRepository candidats;
    private final AffectationRoleRepository affectations;
    private final AlerteEnvoyeeRepository alertes;
    private final EnvoyeurMail mail;
    private final EnvoyeurSms sms;
    private final JournalAlertes journal;
    private final Clock clock;
    private final boolean smsActif;
    private final String urlFront;

    public RappelsResultatsExamenService(CandidatExamenRepository candidats, AffectationRoleRepository affectations,
                                         AlerteEnvoyeeRepository alertes, EnvoyeurMail mail, EnvoyeurSms sms,
                                         JournalAlertes journal, Clock clock,
                                         @Value("${app.sms.alertes:true}") boolean smsActif,
                                         @Value("${app.mail.url-front:http://localhost:5173}") String urlFront) {
        this.candidats = candidats;
        this.affectations = affectations;
        this.alertes = alertes;
        this.mail = mail;
        this.sms = sms;
        this.journal = journal;
        this.clock = clock;
        this.smsActif = smsActif;
        this.urlFront = urlFront;
    }

    /** @return le nombre de couples (examen, établissement) rappelés */
    public int rappeler() {
        Instant maintenant = Instant.now(clock);
        List<CandidatExamen> manquants = candidats.sansResultats(LocalDate.now(clock).minusDays(JOURS_APRES_DEBUT));
        Map<String, CandidatExamen> couples = new LinkedHashMap<>();
        manquants.forEach(c -> couples.putIfAbsent(c.getExamen().getId() + "-" + c.getEtablissement().getId(), c));
        if (couples.isEmpty()) {
            return 0;
        }
        Map<Long, List<AffectationRole>> directions = new java.util.HashMap<>();
        affectations.directionsDe(couples.values().stream().map(c -> c.getEtablissement().getId()).distinct().toList())
                .forEach(a -> directions.computeIfAbsent(a.getEtablissement().getId(), k -> new java.util.ArrayList<>()).add(a));

        int rappeles = 0;
        for (Map.Entry<String, CandidatExamen> e : couples.entrySet()) {
            String cle = "EXA#" + e.getKey() + " ";
            if (alertes.existsByTypeAndResumeStartingWithAndReussieTrueAndEnvoyeeLeAfter(TYPE, cle,
                    maintenant.minus(ENTRE_DEUX_RAPPELS))) {
                continue;
            }
            CandidatExamen c = e.getValue();
            String resume = cle + "Résultats de l'examen « " + c.getExamen().getLibelle() + " » non transmis pour "
                    + c.getEtablissement().getNom();
            boolean prevenu = false;
            for (AffectationRole a : directions.getOrDefault(c.getEtablissement().getId(), List.of())) {
                prevenu |= prevenir(a, resume);
            }
            if (prevenu) {
                rappeles++;
            }
        }
        return rappeles;
    }

    private boolean prevenir(AffectationRole a, String resume) {
        boolean envoye = false;
        String texte = resume.substring(resume.indexOf(' ') + 1);
        String email = a.getUtilisateur().getEmail();
        if (email != null) {
            boolean ok = false;
            try {
                mail.envoyer(email, "Résultats d'examen non transmis",
                        "Bonjour,\n\n" + texte + ".\n\nMerci de les transmettre à la zone qui organise l'examen : " + urlFront + "\n");
                ok = true;
            } catch (RuntimeException ex) {
                log.warn("E-mail de rappel non envoyé : {}", ex.getMessage());
            }
            journal.noter(TYPE, "EMAIL", email, resume, ok);
            envoye |= ok;
        }
        String tel = a.getUtilisateur().getTelephone();
        if (smsActif && tel != null) {
            boolean ok = false;
            try {
                ok = sms.envoyer(tel, "Plateforme scolaire : " + texte);
            } catch (RuntimeException ex) {
                log.warn("SMS de rappel non envoyé : {}", ex.getMessage());
            }
            journal.noter(TYPE, "SMS", tel, resume, ok);
            envoye |= ok;
        }
        return envoye;
    }
}
