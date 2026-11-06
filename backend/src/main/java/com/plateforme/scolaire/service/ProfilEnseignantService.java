package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.Enseignant;
import com.plateforme.scolaire.domain.Etablissement;
import com.plateforme.scolaire.domain.Matiere;
import com.plateforme.scolaire.domain.ProfilEnseignant;
import com.plateforme.scolaire.domain.Role;
import com.plateforme.scolaire.repository.EnseignantRepository;
import com.plateforme.scolaire.repository.EtablissementRepository;
import com.plateforme.scolaire.repository.MatiereRepository;
import com.plateforme.scolaire.repository.ProfilEnseignantRepository;
import com.plateforme.scolaire.security.PerimetreService;
import com.plateforme.scolaire.security.UtilisateurPrincipal;
import com.plateforme.scolaire.web.dto.ProfilEnseignantDto;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Profils professionnels des enseignants. La direction de l'établissement renseigne la fiche ; le ministère et les directions
 * régionales ne voient que des totaux par établissement (formés, non titulaires, postes à pourvoir).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ProfilEnseignantService {

    private final ProfilEnseignantRepository profils;
    private final EnseignantRepository enseignants;
    private final EtablissementRepository etablissements;
    private final MatiereRepository matieres;
    private final PerimetreService perimetre;
    private final AuditService audit;
    private final Clock clock;

    /** Pur : âge en années révolues ; null sans date de naissance. */
    static Integer age(LocalDate naissance, LocalDate aujourdhui) {
        return naissance == null ? null : Period.between(naissance, aujourdhui).getYears();
    }

    /** Pur : un profil est renseigné quand le statut et la formation académique sont connus. */
    static boolean renseigne(ProfilEnseignant p) {
        return p != null && p.getStatut() != null && p.getFormationAcademique() != null;
    }

    /** Pur : enseignant formé au métier = a suivi une formation professionnelle (ENI, ENS ou autre). */
    static boolean forme(ProfilEnseignant p) {
        return p != null && p.getFormationProfessionnelle() != null && !"AUCUNE".equals(p.getFormationProfessionnelle());
    }

    /** Pur : postes à pourvoir ; null tant que les postes ne sont pas renseignés. */
    static Integer deficit(Integer postes, int enseignants) {
        return postes == null ? null : Math.max(0, postes - enseignants);
    }

    /** Pur : cohérence de la fiche (prise de service après la naissance, au moins 18 ans à la prise de service). */
    static String verifier(ProfilEnseignantDto.Requete q) {
        if (q.dateNaissance() != null && q.datePriseService() != null) {
            if (!q.datePriseService().isAfter(q.dateNaissance())) {
                return "La prise de service doit suivre la date de naissance";
            }
            if (Period.between(q.dateNaissance(), q.datePriseService()).getYears() < 18) {
                return "L'enseignant doit avoir au moins 18 ans à sa prise de service";
            }
        }
        return null;
    }

    private ProfilEnseignantDto.Profil dto(Enseignant e, ProfilEnseignant p, Map<Long, String> libellesMatieres) {
        LocalDate aujourdhui = LocalDate.now(clock);
        return new ProfilEnseignantDto.Profil(e.getId(), e.getUtilisateur().getNom(), e.getUtilisateur().getPrenom(), e.getMatricule(),
                p == null ? null : p.getDateNaissance(), p == null ? null : age(p.getDateNaissance(), aujourdhui), p == null ? null : p.getSexe(),
                p == null ? null : p.getStatut(), p == null ? null : p.getFormationAcademique(), p == null ? null : p.getFormationProfessionnelle(),
                p == null ? null : p.getMatierePrincipaleId(), p == null || p.getMatierePrincipaleId() == null ? null : libellesMatieres.get(p.getMatierePrincipaleId()),
                p == null ? null : p.getDatePriseService(), p == null ? null : p.getHandicap(), renseigne(p), forme(p));
    }

    private Map<Long, String> libellesMatieres() {
        Map<Long, String> res = new HashMap<>();
        for (Matiere m : matieres.findAll()) {
            res.put(m.getId(), m.getLibelle());
        }
        return res;
    }

    @Transactional(readOnly = true)
    public List<ProfilEnseignantDto.Profil> lister(Long etablissementId) {
        if (!etablissements.existsById(etablissementId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Établissement introuvable");
        }
        List<Enseignant> liste = enseignants.findByEtablissement(etablissementId, Role.ENSEIGNANT);
        Map<Long, ProfilEnseignant> parEnseignant = new HashMap<>();
        if (!liste.isEmpty()) {
            profils.findByEnseignantIdIn(liste.stream().map(Enseignant::getId).toList()).forEach(p -> parEnseignant.put(p.getEnseignantId(), p));
        }
        Map<Long, String> libelles = libellesMatieres();
        return liste.stream().map(e -> dto(e, parEnseignant.get(e.getId()), libelles)).toList();
    }

    public ProfilEnseignantDto.Profil enregistrer(Long etablissementId, Long enseignantId, ProfilEnseignantDto.Requete q, String auteur) {
        Enseignant e = enseignants.findById(enseignantId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Enseignant introuvable"));
        if (enseignants.compterRattachements(enseignantId, etablissementId, Role.ENSEIGNANT) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cet enseignant ne travaille pas dans cet établissement");
        }
        String message = verifier(q);
        if (message != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        if (q.matierePrincipaleId() != null && !matieres.existsById(q.matierePrincipaleId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Matière inconnue");
        }
        ProfilEnseignant p = profils.findById(enseignantId).orElseGet(() -> {
            ProfilEnseignant n = new ProfilEnseignant();
            n.setEnseignantId(enseignantId);
            return n;
        });
        p.setDateNaissance(q.dateNaissance());
        p.setSexe(q.sexe());
        p.setStatut(q.statut());
        p.setFormationAcademique(q.formationAcademique());
        p.setFormationProfessionnelle(q.formationProfessionnelle());
        p.setMatierePrincipaleId(q.matierePrincipaleId());
        p.setDatePriseService(q.datePriseService());
        p.setHandicap(q.handicap());
        p.setMisAJourLe(Instant.now(clock));
        p.setMisAJourPar(auteur);
        profils.save(p);
        audit.enregistrer("PROFIL_ENSEIGNANT_MIS_A_JOUR", "Enseignant", enseignantId, Map.of("etablissement", etablissementId));
        return dto(e, p, libellesMatieres());
    }

    /** Totaux par établissement du périmètre de l'utilisateur ; aucune donnée nominative. */
    @Transactional(readOnly = true)
    public ProfilEnseignantDto.Synthese synthese() {
        UtilisateurPrincipal u = perimetre.utilisateurCourant();
        if (u == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }
        List<ProfilEnseignantDto.Ligne> lignes = new ArrayList<>();
        int total = 0;
        int renseignes = 0;
        int formes = 0;
        int nonTitulaires = 0;
        int femmes = 0;
        int deficitTotal = 0;
        for (Etablissement e : perimetre.etablissementsAccessibles(u)) {
            List<Enseignant> liste = enseignants.findByEtablissement(e.getId(), Role.ENSEIGNANT);
            Map<Long, ProfilEnseignant> parEnseignant = new HashMap<>();
            if (!liste.isEmpty()) {
                profils.findByEnseignantIdIn(liste.stream().map(Enseignant::getId).toList()).forEach(p -> parEnseignant.put(p.getEnseignantId(), p));
            }
            int r = 0;
            int f = 0;
            int nt = 0;
            for (Enseignant en : liste) {
                ProfilEnseignant p = parEnseignant.get(en.getId());
                if (renseigne(p)) {
                    r++;
                }
                if (forme(p)) {
                    f++;
                }
                if (p != null && p.getStatut() != null && !"FONCTIONNAIRE".equals(p.getStatut())) {
                    nt++;
                }
                if (p != null && "F".equals(p.getSexe())) {
                    femmes++;
                }
            }
            Integer deficit = deficit(e.getPostesEnseignants(), liste.size());
            if (deficit != null) {
                deficitTotal += deficit;
            }
            total += liste.size();
            renseignes += r;
            formes += f;
            nonTitulaires += nt;
            lignes.add(new ProfilEnseignantDto.Ligne(e.getId(), e.getNom(), e.getZone() == null ? null : e.getZone().getNom(), liste.size(), r, f, nt,
                    e.getPostesEnseignants(), deficit));
        }
        lignes.sort(Comparator.comparing((ProfilEnseignantDto.Ligne l) -> l.deficit() == null ? -1 : l.deficit()).reversed()
                .thenComparing(ProfilEnseignantDto.Ligne::etablissement));
        return new ProfilEnseignantDto.Synthese(new ProfilEnseignantDto.Resume(lignes.size(), total, renseignes, formes, nonTitulaires, femmes, deficitTotal), lignes);
    }
}
