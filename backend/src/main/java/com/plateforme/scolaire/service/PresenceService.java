package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.AnneeScolaire;
import com.plateforme.scolaire.domain.Classe;
import com.plateforme.scolaire.domain.Inscription;
import com.plateforme.scolaire.domain.Presence;
import com.plateforme.scolaire.domain.Utilisateur;
import com.plateforme.scolaire.repository.ClasseRepository;
import com.plateforme.scolaire.repository.InscriptionRepository;
import com.plateforme.scolaire.repository.PresenceRepository;
import com.plateforme.scolaire.repository.UtilisateurRepository;
import com.plateforme.scolaire.security.PerimetreService;
import com.plateforme.scolaire.security.UtilisateurPrincipal;
import com.plateforme.scolaire.web.dto.AppelRequest;
import com.plateforme.scolaire.web.dto.EleveDto;
import com.plateforme.scolaire.web.dto.FeuilleAppelDto;
import com.plateforme.scolaire.web.dto.LigneFeuilleDto;
import com.plateforme.scolaire.web.dto.LignePresenceRequest;
import com.plateforme.scolaire.web.dto.PresenceDto;
import com.plateforme.scolaire.web.dto.StatutPresence;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Appel quotidien par classe. Les autorisations (@PreAuthorize) sont vérifiées dans les contrôleurs ;
 * ce service applique les règles métier.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PresenceService {

    private static final int DUREE_MAX_HISTORIQUE_JOURS = 366;

    private final PresenceRepository presenceRepository;
    private final InscriptionRepository inscriptionRepository;
    private final ClasseRepository classeRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final PerimetreService perimetreService;
    private final Clock clock;
    private final AuditService auditService;

    /** Feuille d'appel du jour (ou de la date demandée) : tous les élèves acceptés de la classe. */
    @Transactional(readOnly = true)
    public FeuilleAppelDto feuille(Long classeId, LocalDate date) {
        charger(classeId);
        LocalDate jour = date != null ? date : LocalDate.now(clock);
        return construire(classeId, jour, inscrits(classeId), presencesDuJour(classeId, jour));
    }

    /** Enregistre (ou corrige) l'appel d'une journée. Idempotent : renvoyer la même requête ne change rien. */
    public FeuilleAppelDto enregistrer(Long classeId, LocalDate date, AppelRequest requete) {
        Classe classe = charger(classeId);
        if (date.isAfter(LocalDate.now(clock))) {
            throw invalide("On ne peut pas saisir une présence dans le futur");
        }
        AnneeScolaire annee = classe.getAnneeScolaire();
        if (date.isBefore(annee.getDateDebut()) || date.isAfter(annee.getDateFin())) {
            throw invalide("La date est en dehors de l'année scolaire de la classe");
        }

        List<Inscription> inscrits = inscrits(classeId);
        Map<Long, Inscription> inscritParId = new LinkedHashMap<>();
        inscrits.forEach(i -> inscritParId.put(i.getId(), i));

        Set<Long> vus = new HashSet<>();
        for (LignePresenceRequest ligne : requete.lignes()) {
            if (!inscritParId.containsKey(ligne.inscriptionId())) {
                throw invalide("L'inscription " + ligne.inscriptionId() + " n'est pas un élève accepté de cette classe");
            }
            if (!vus.add(ligne.inscriptionId())) {
                throw invalide("L'inscription " + ligne.inscriptionId() + " apparaît plusieurs fois");
            }
        }

        Map<Long, Presence> presences = presencesDuJour(classeId, date);
        UtilisateurPrincipal courant = perimetreService.utilisateurCourant();
        Utilisateur auteur = courant != null ? utilisateurRepository.getReferenceById(courant.getId()) : null;

        int creees = 0;
        List<Presence> corrigees = new ArrayList<>();
        List<String> avant = new ArrayList<>();
        for (LignePresenceRequest ligne : requete.lignes()) {
            Presence presence = presences.get(ligne.inscriptionId());
            boolean nonPresent = ligne.statut() != StatutPresence.PRESENT;
            String motif = nonPresent ? nettoyer(ligne.motif()) : null;
            boolean justifie = nonPresent && Boolean.TRUE.equals(ligne.justifie());
            if (presence == null) {
                presence = new Presence();
                presence.setInscription(inscritParId.get(ligne.inscriptionId()));
                presence.setDateSeance(date);
                creees++;
            } else if (!presence.getStatut().equals(ligne.statut().name()) || presence.isJustifie() != justifie
                    || !Objects.equals(presence.getMotif(), motif)) {
                corrigees.add(presence);
                avant.add(presence.getStatut() + (presence.isJustifie() ? " (justifié)" : ""));
            }
            presence.setStatut(ligne.statut().name());
            presence.setMotif(motif);
            presence.setJustifie(justifie);
            presence.setSaisiPar(auteur);
            presences.put(ligne.inscriptionId(), presenceRepository.save(presence));
        }
        journaliser(classeId, date, requete, presences, creees, corrigees, avant);
        return construire(classeId, date, inscrits, presences);
    }

    /** Historique des présences d'un élève (30 derniers jours par défaut, 366 jours au maximum). */
    @Transactional(readOnly = true)
    public List<PresenceDto> historiqueEleve(Long eleveId, LocalDate du, LocalDate au) {
        LocalDate fin = au != null ? au : LocalDate.now(clock);
        LocalDate debut = du != null ? du : fin.minusDays(30);
        if (debut.isAfter(fin)) {
            throw invalide("La date de début doit précéder la date de fin");
        }
        if (ChronoUnit.DAYS.between(debut, fin) > DUREE_MAX_HISTORIQUE_JOURS) {
            throw invalide("La période demandée ne peut pas dépasser " + DUREE_MAX_HISTORIQUE_JOURS + " jours");
        }
        return presenceRepository.findByInscriptionEleveIdAndDateSeanceBetweenOrderByDateSeanceDesc(eleveId, debut, fin)
                .stream().map(PresenceDto::de).toList();
    }

    // ---------- Interne ----------

    /**
     * Une entrée par appel réellement modifié (renvoyer la même requête ne laisse aucune trace), plus une
     * entrée par présence corrigée : c'est la correction d'une absence déjà saisie qu'il faut pouvoir retrouver.
     */
    private void journaliser(Long classeId, LocalDate date, AppelRequest requete, Map<Long, Presence> presences,
                             int creees, List<Presence> corrigees, List<String> avant) {
        if (creees == 0 && corrigees.isEmpty()) {
            return;
        }
        int presents = 0;
        int absents = 0;
        int retards = 0;
        for (LignePresenceRequest ligne : requete.lignes()) {
            switch (ligne.statut()) {
                case PRESENT -> presents++;
                case ABSENT -> absents++;
                case RETARD -> retards++;
            }
        }
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("classeId", classeId);
        details.put("date", date.toString());
        details.put("presents", presents);
        details.put("absents", absents);
        details.put("retards", retards);
        details.put("creees", creees);
        details.put("corrigees", corrigees.size());
        auditService.enregistrer("SAISIE_APPEL", "classe", classeId, details);

        for (int i = 0; i < corrigees.size(); i++) {
            Presence p = corrigees.get(i);
            Map<String, Object> correction = new LinkedHashMap<>();
            correction.put("inscriptionId", p.getInscription().getId());
            correction.put("date", date.toString());
            correction.put("avant", avant.get(i));
            correction.put("apres", p.getStatut() + (p.isJustifie() ? " (justifié)" : ""));
            auditService.enregistrer("CORRECTION_PRESENCE", "presence", p.getId(), correction);
        }
    }

    private Classe charger(Long classeId) {
        return classeRepository.findById(classeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Classe introuvable"));
    }

    private List<Inscription> inscrits(Long classeId) {
        return inscriptionRepository.findByClasseIdAndStatutOrderByEleveNomAscElevePrenomAsc(classeId, "ACCEPTEE");
    }

    private Map<Long, Presence> presencesDuJour(Long classeId, LocalDate date) {
        Map<Long, Presence> parInscription = new LinkedHashMap<>();
        presenceRepository.findByInscriptionClasseIdAndDateSeance(classeId, date)
                .forEach(p -> parInscription.put(p.getInscription().getId(), p));
        return parInscription;
    }

    private FeuilleAppelDto construire(Long classeId, LocalDate date, List<Inscription> inscrits,
                                       Map<Long, Presence> presences) {
        List<LigneFeuilleDto> lignes = inscrits.stream().map(i -> {
            Presence p = presences.get(i.getId());
            return new LigneFeuilleDto(
                    i.getId(),
                    EleveDto.de(i.getEleve()),
                    p != null ? StatutPresence.valueOf(p.getStatut()) : null,
                    p != null ? p.getMotif() : null,
                    p != null && p.isJustifie());
        }).toList();
        return new FeuilleAppelDto(classeId, date, lignes);
    }

    private static String nettoyer(String motif) {
        return motif == null || motif.isBlank() ? null : motif.trim();
    }

    private static ResponseStatusException invalide(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
