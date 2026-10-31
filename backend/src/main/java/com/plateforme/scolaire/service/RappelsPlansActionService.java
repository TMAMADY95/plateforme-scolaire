package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.AffectationRole;
import com.plateforme.scolaire.domain.PlanAction;
import com.plateforme.scolaire.repository.AffectationRoleRepository;
import com.plateforme.scolaire.repository.PlanActionRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Rappelle à la direction de l'établissement les plans d'action dont l'échéance approche (3 jours) ou est dépassée,
 * une fois par semaine au plus pour chaque plan. Chaque envoi est noté dans le journal des alertes.
 */
@Slf4j
@Service
@Transactional
public class RappelsPlansActionService {

    static final int JOURS_AVANT_ECHEANCE = 3;
    static final Duration ENTRE_DEUX_RAPPELS = Duration.ofDays(7);

    private final PlanActionRepository planRepository;
    private final AffectationRoleRepository affectationRepository;
    private final EnvoyeurMail envoyeurMail;
    private final EnvoyeurSms envoyeurSms;
    private final JournalAlertes journal;
    private final Clock clock;
    private final boolean smsActif;
    private final String urlFront;

    public RappelsPlansActionService(PlanActionRepository planRepository, AffectationRoleRepository affectationRepository,
                                     EnvoyeurMail envoyeurMail, EnvoyeurSms envoyeurSms, JournalAlertes journal,
                                     Clock clock, @Value("${app.sms.alertes:true}") boolean smsActif,
                                     @Value("${app.mail.url-front:http://localhost:5173}") String urlFront) {
        this.planRepository = planRepository;
        this.affectationRepository = affectationRepository;
        this.envoyeurMail = envoyeurMail;
        this.envoyeurSms = envoyeurSms;
        this.journal = journal;
        this.clock = clock;
        this.smsActif = smsActif;
        this.urlFront = urlFront;
    }

    /** @return le nombre de plans rappelés */
    public int rappeler() {
        Instant maintenant = Instant.now(clock);
        LocalDate aujourdhui = LocalDate.now(clock);
        List<PlanAction> plans = planRepository.aRappeler(aujourdhui.plusDays(JOURS_AVANT_ECHEANCE),
                maintenant.minus(ENTRE_DEUX_RAPPELS));
        if (plans.isEmpty()) {
            return 0;
        }
        Map<Long, List<AffectationRole>> directions = new HashMap<>();
        affectationRepository.directionsDe(plans.stream().map(p -> p.getEtablissement().getId()).distinct().toList())
                .forEach(a -> directions.computeIfAbsent(a.getEtablissement().getId(), k -> new java.util.ArrayList<>()).add(a));

        int rappeles = 0;
        for (PlanAction p : plans) {
            boolean retard = p.getEcheance().isBefore(aujourdhui);
            String resume = (retard ? "Plan d'action en retard : " : "Échéance proche : ") + abrege(p.getDescription())
                    + " (au " + p.getEcheance() + ")";
            boolean prevenu = false;
            for (AffectationRole a : directions.getOrDefault(p.getEtablissement().getId(), List.of())) {
                prevenu |= prevenir(a, p, resume);
            }
            if (prevenu) {
                p.setDernierRappel(maintenant);
                rappeles++;
            }
        }
        return rappeles;
    }

    private boolean prevenir(AffectationRole a, PlanAction p, String resume) {
        boolean envoye = false;
        String email = a.getUtilisateur().getEmail();
        if (email != null) {
            boolean ok = false;
            try {
                envoyeurMail.envoyer(email, "Plan d'action : " + p.getEtablissement().getNom(),
                        "Bonjour,\n\n" + resume + ".\n\nÉtablissement : " + p.getEtablissement().getNom()
                                + "\nConnectez-vous pour indiquer l'avancement : " + urlFront + "\n");
                ok = true;
            } catch (RuntimeException e) {
                log.warn("E-mail de rappel non envoyé : {}", e.getMessage());
            }
            journal.noter("PLAN_ACTION", "EMAIL", email, resume, ok);
            envoye |= ok;
        }
        String tel = a.getUtilisateur().getTelephone();
        if (smsActif && tel != null) {
            boolean ok = false;
            try {
                ok = envoyeurSms.envoyer(tel, "Plateforme scolaire : " + resume);
            } catch (RuntimeException e) {
                log.warn("SMS de rappel non envoyé : {}", e.getMessage());
            }
            journal.noter("PLAN_ACTION", "SMS", tel, resume, ok);
            envoye |= ok;
        }
        return envoye;
    }

    private static String abrege(String texte) {
        return texte.length() <= 80 ? texte : texte.substring(0, 79) + "…";
    }
}
