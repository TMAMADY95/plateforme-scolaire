package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.AffectationRole;
import com.plateforme.scolaire.domain.AlerteUrgenceEtat;
import com.plateforme.scolaire.domain.Etablissement;
import com.plateforme.scolaire.domain.Perimetre;
import com.plateforme.scolaire.domain.Role;
import com.plateforme.scolaire.repository.AffectationRoleRepository;
import com.plateforme.scolaire.repository.AlerteUrgenceEtatRepository;
import com.plateforme.scolaire.repository.EtablissementRepository;
import com.plateforme.scolaire.web.dto.LigneConformiteDto;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Prévient le ministère (tout le pays) et chaque direction régionale (ses zones) des établissements qui
 * nécessitent une inspection urgente, par e-mail et par SMS. Un établissement n'est signalé que lorsqu'il devient
 * urgent, puis rappelé une fois par semaine tant qu'il le reste : pas de message quotidien identique.
 */
@Slf4j
@Service
@Transactional
public class AlertesUrgenceService {

    static final Duration RAPPEL = Duration.ofDays(7);
    static final int MAX_NOMS = 10;

    private final ConformiteService conformiteService;
    private final AffectationRoleRepository affectationRepository;
    private final EtablissementRepository etablissementRepository;
    private final AlerteUrgenceEtatRepository etatRepository;
    private final EnvoyeurMail envoyeurMail;
    private final EnvoyeurSms envoyeurSms;
    private final JournalAlertes journal;
    private final Clock clock;
    private final boolean smsActif;
    private final String urlFront;

    public AlertesUrgenceService(ConformiteService conformiteService, AffectationRoleRepository affectationRepository,
                                 EtablissementRepository etablissementRepository,
                                 AlerteUrgenceEtatRepository etatRepository, EnvoyeurMail envoyeurMail,
                                 EnvoyeurSms envoyeurSms, JournalAlertes journal, Clock clock,
                                 @Value("${app.sms.alertes:true}") boolean smsActif,
                                 @Value("${app.mail.url-front:http://localhost:5173}") String urlFront) {
        this.conformiteService = conformiteService;
        this.affectationRepository = affectationRepository;
        this.etablissementRepository = etablissementRepository;
        this.etatRepository = etatRepository;
        this.envoyeurMail = envoyeurMail;
        this.envoyeurSms = envoyeurSms;
        this.journal = journal;
        this.clock = clock;
        this.smsActif = smsActif;
        this.urlFront = urlFront;
    }

    private record Destinataire(String email, String telephone, Set<Long> etablissements) {
        boolean tout() {
            return etablissements == null;
        }
    }

    /** @return le nombre d'établissements signalés cette fois-ci */
    public int alerter() {
        Instant maintenant = Instant.now(clock);
        List<LigneConformiteDto> lignes = conformiteService.nationale(null, null, null, null).lignes();
        Map<Long, AlerteUrgenceEtat> etats = new HashMap<>();
        etatRepository.findAll().forEach(e -> etats.put(e.getEtablissementId(), e));

        List<LigneConformiteDto> aSignaler = new ArrayList<>();
        for (LigneConformiteDto l : lignes) {
            AlerteUrgenceEtat etat = etats.get(l.id());
            boolean neuf = etat == null || !etat.isUrgent() || etat.getDerniereAlerte() == null
                    || etat.getDerniereAlerte().isBefore(maintenant.minus(RAPPEL));
            if (l.urgent() && neuf) {
                aSignaler.add(l);
            }
        }

        Map<Long, Destinataire> destinataires = destinataires();
        Set<Long> signalees = new HashSet<>();
        for (Destinataire d : destinataires.values()) {
            List<LigneConformiteDto> siennes = aSignaler.stream()
                    .filter(l -> d.tout() || d.etablissements().contains(l.id())).toList();
            if (!siennes.isEmpty() && prevenir(d, siennes)) {
                siennes.forEach(l -> signalees.add(l.id()));
            }
        }

        for (LigneConformiteDto l : lignes) {
            AlerteUrgenceEtat etat = etats.computeIfAbsent(l.id(), AlerteUrgenceEtat::new);
            etat.setUrgent(l.urgent());
            if (signalees.contains(l.id())) {
                etat.setDerniereAlerte(maintenant);
            }
            etatRepository.save(etat);
        }
        return signalees.size();
    }

    /** Un destinataire par compte : le ministère voit tout, une direction régionale voit ses zones. */
    private Map<Long, Destinataire> destinataires() {
        Map<Long, Destinataire> resultat = new LinkedHashMap<>();
        Map<Long, Set<Long>> parZone = new HashMap<>();
        for (AffectationRole a : affectationRepository.destinataires(EnumSet.of(Role.MINISTERE, Role.DIRECTION_REGIONALE))) {
            Long compte = a.getUtilisateur().getId();
            String email = a.getUtilisateur().getEmail();
            String tel = a.getUtilisateur().getTelephone();
            if (a.getRole() == Role.MINISTERE) {
                resultat.put(compte, new Destinataire(email, tel, null));
            } else if (a.getPerimetre() == Perimetre.ZONE && a.getZone() != null) {
                Set<Long> ids = parZone.computeIfAbsent(a.getZone().getId(), z -> {
                    Set<Long> s = new HashSet<>();
                    etablissementRepository.findByZoneEtSousZones(z).stream().map(Etablissement::getId).forEach(s::add);
                    return s;
                });
                Destinataire existant = resultat.get(compte);
                if (existant == null) {
                    resultat.put(compte, new Destinataire(email, tel, new HashSet<>(ids)));
                } else if (!existant.tout()) {
                    existant.etablissements().addAll(ids);
                }
            }
        }
        return resultat;
    }

    private static String resume(List<LigneConformiteDto> lignes) {
        String noms = lignes.stream().limit(3).map(LigneConformiteDto::nom).collect(java.util.stream.Collectors.joining(", "));
        return lignes.size() + " établissement(s) à inspecter d'urgence : " + noms + (lignes.size() > 3 ? "…" : "");
    }

    private boolean prevenir(Destinataire d, List<LigneConformiteDto> lignes) {
        boolean envoye = false;
        int n = lignes.size();
        if (d.email() != null) {
            StringBuilder texte = new StringBuilder("Bonjour,\n\n")
                    .append(n).append(n > 1 ? " établissements nécessitent" : " établissement nécessite")
                    .append(" une inspection urgente :\n\n");
            lignes.stream().limit(MAX_NOMS).forEach(l -> texte.append("- ").append(l.nom())
                    .append(l.zone() == null ? "" : " (" + l.zone() + ")")
                    .append(" : ").append(String.join(", ", l.motifsUrgence())).append('\n'));
            if (n > MAX_NOMS) {
                texte.append("… et ").append(n - MAX_NOMS).append(" autre(s).\n");
            }
            texte.append("\nDétail : ").append(urlFront).append('\n');
            boolean ok = false;
            try {
                envoyeurMail.envoyer(d.email(), "Alerte : " + n + " établissement(s) à inspecter d'urgence", texte.toString());
                ok = true;
            } catch (RuntimeException e) {
                log.warn("E-mail d'alerte non envoyé : {}", e.getMessage());
            }
            journal.noter("URGENCE", "EMAIL", d.email(), resume(lignes), ok);
            envoye |= ok;
        }
        if (smsActif && d.telephone() != null) {
            boolean ok = false;
            try {
                ok = envoyeurSms.envoyer(d.telephone(),
                        "Plateforme scolaire : " + n + " établissement(s) à inspecter d'urgence. Connectez-vous pour le détail.");
            } catch (RuntimeException e) {
                log.warn("SMS d'alerte non envoyé : {}", e.getMessage());
            }
            journal.noter("URGENCE", "SMS", d.telephone(), resume(lignes), ok);
            envoye |= ok;
        }
        return envoye;
    }
}
