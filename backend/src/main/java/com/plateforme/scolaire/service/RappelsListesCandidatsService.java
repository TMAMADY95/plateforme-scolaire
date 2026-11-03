package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.AffectationRole;
import com.plateforme.scolaire.domain.ListeCandidats;
import com.plateforme.scolaire.domain.Role;
import com.plateforme.scolaire.domain.ZoneGeographique;
import com.plateforme.scolaire.repository.AffectationRoleRepository;
import com.plateforme.scolaire.repository.AlerteEnvoyeeRepository;
import com.plateforme.scolaire.repository.ListeCandidatsRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Rappelle aux responsables les listes de candidats qui attendent depuis plus de 7 jours : les listes transmises à la
 * direction préfectorale ou communale de la zone (et, au bout de 14 jours, à l'inspection régionale), les listes vérifiées
 * à la DGECS. Un seul message par responsable et par semaine, qui regroupe ses établissements. Aucun rappel une fois les
 * épreuves commencées.
 */
@Slf4j
@Service
@Transactional
public class RappelsListesCandidatsService {

    static final int JOURS_ATTENTE = 7;
    static final int JOURS_ESCALADE = 14;
    static final Duration ENTRE_DEUX_RAPPELS = Duration.ofDays(7);
    static final String TYPE = "LISTE_CANDIDATS";
    static final int MAX_LIGNES = 10;

    private final ListeCandidatsRepository listes;
    private final AffectationRoleRepository affectations;
    private final AlerteEnvoyeeRepository alertes;
    private final EnvoyeurMail mail;
    private final EnvoyeurSms sms;
    private final JournalAlertes journal;
    private final Clock clock;
    private final boolean smsActif;
    private final String urlFront;

    public RappelsListesCandidatsService(ListeCandidatsRepository listes, AffectationRoleRepository affectations,
                                         AlerteEnvoyeeRepository alertes, EnvoyeurMail mail, EnvoyeurSms sms, JournalAlertes journal, Clock clock,
                                         @Value("${app.sms.alertes:true}") boolean smsActif,
                                         @Value("${app.mail.url-front:http://localhost:5173}") String urlFront) {
        this.listes = listes;
        this.affectations = affectations;
        this.alertes = alertes;
        this.mail = mail;
        this.sms = sms;
        this.smsActif = smsActif;
        this.journal = journal;
        this.clock = clock;
        this.urlFront = urlFront;
    }

    /** Pur : rôles à prévenir pour une liste dans cet état depuis ce nombre de jours (vide si le délai n'est pas atteint). */
    static Set<Role> rolesAPrevenir(String statut, long jours) {
        if (jours < JOURS_ATTENTE) {
            return Set.of();
        }
        return switch (statut) {
            case "TRANSMISE" -> jours >= JOURS_ESCALADE ? Set.of(Role.DIRECTION_LOCALE, Role.DIRECTION_REGIONALE) : Set.of(Role.DIRECTION_LOCALE);
            case "VALIDEE_REGION" -> Set.of(Role.DGECS);
            default -> Set.of();
        };
    }

    /** Pur : le responsable est concerné si sa zone contient celle de l'établissement (la DGECS est nationale). */
    static boolean concerne(Role role, ZoneGeographique zoneResponsable, ZoneGeographique zoneEtablissement) {
        if (role == Role.DGECS) {
            return true;
        }
        if (zoneResponsable == null) {
            return false;
        }
        for (ZoneGeographique z = zoneEtablissement; z != null; z = z.getParent()) {
            if (zoneResponsable.getId().equals(z.getId())) {
                return true;
            }
        }
        return false;
    }

    /** @return le nombre de responsables rappelés */
    public int rappeler() {
        Instant maintenant = Instant.now(clock);
        LocalDate aujourdhui = LocalDate.now(clock);
        List<ListeCandidats> attente = listes.enEtat(List.of("TRANSMISE", "VALIDEE_REGION")).stream()
                .filter(l -> l.getExamen().getDateDebut() == null || !l.getExamen().getDateDebut().isBefore(aujourdhui)).toList();
        if (attente.isEmpty()) {
            return 0;
        }
        List<AffectationRole> responsables = affectations.destinataires(List.of(Role.DGECS, Role.DIRECTION_REGIONALE, Role.DIRECTION_LOCALE));
        Map<Long, List<String>> lignes = new LinkedHashMap<>();
        Map<Long, String> emails = new LinkedHashMap<>();
        Map<Long, String> telephones = new LinkedHashMap<>();
        for (ListeCandidats l : attente) {
            Instant depuis = "TRANSMISE".equals(l.getStatut()) ? l.getTransmiseLe() : l.getValideeRegionLe();
            if (depuis == null) {
                continue;
            }
            long jours = Duration.between(depuis, maintenant).toDays();
            Set<Role> roles = rolesAPrevenir(l.getStatut(), jours);
            for (AffectationRole a : responsables) {
                if (roles.contains(a.getRole()) && concerne(a.getRole(), a.getZone(), l.getEtablissement().getZone())) {
                    Long u = a.getUtilisateur().getId();
                    emails.putIfAbsent(u, a.getUtilisateur().getEmail());
                    telephones.putIfAbsent(u, a.getUtilisateur().getTelephone());
                    lignes.computeIfAbsent(u, k -> new ArrayList<>()).add(l.getEtablissement().getNom() + " (« " + l.getExamen().getLibelle()
                            + " », " + ("TRANSMISE".equals(l.getStatut()) ? "à vérifier" : "à valider") + " depuis " + jours + " jours)");
                }
            }
        }
        int rappeles = 0;
        for (Map.Entry<Long, List<String>> e : lignes.entrySet()) {
            String cle = "LST#" + e.getKey() + " ";
            String email = emails.get(e.getKey());
            String telephone = telephones.get(e.getKey());
            if ((email == null && (!smsActif || telephone == null)) || alertes.existsByTypeAndResumeStartingWithAndReussieTrueAndEnvoyeeLeAfter(TYPE, cle,
                    maintenant.minus(ENTRE_DEUX_RAPPELS))) {
                continue;
            }
            List<String> l = e.getValue();
            String detail = String.join("\n- ", l.subList(0, Math.min(MAX_LIGNES, l.size())))
                    + (l.size() > MAX_LIGNES ? "\n- … et " + (l.size() - MAX_LIGNES) + " autre(s)" : "");
            String resume = cle + l.size() + " liste(s) de candidats en attente de votre action";
            boolean ok = false;
            if (email != null) {
                boolean envoye = false;
                try {
                    mail.envoyer(email, "Listes de candidats en attente",
                            "Bonjour,\n\n" + l.size() + " liste(s) de candidats attendent votre action :\n- " + detail + "\n\nSuivi : " + urlFront + "\n");
                    envoye = true;
                } catch (RuntimeException ex) {
                    log.warn("E-mail de rappel de listes non envoyé : {}", ex.getMessage());
                }
                journal.noter(TYPE, "EMAIL", email, resume, envoye);
                ok |= envoye;
            }
            if (smsActif && telephone != null) {
                boolean envoye = false;
                try {
                    envoye = sms.envoyer(telephone, "Plateforme scolaire : " + resume.substring(cle.length()));
                } catch (RuntimeException ex) {
                    log.warn("SMS de rappel de listes non envoyé : {}", ex.getMessage());
                }
                journal.noter(TYPE, "SMS", telephone, resume, envoye);
                ok |= envoye;
            }
            if (ok) {
                rappeles++;
            }
        }
        return rappeles;
    }
}
