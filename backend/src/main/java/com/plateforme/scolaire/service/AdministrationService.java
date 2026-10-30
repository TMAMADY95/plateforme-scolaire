package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.AffectationRole;
import com.plateforme.scolaire.domain.Etablissement;
import com.plateforme.scolaire.domain.Perimetre;
import com.plateforme.scolaire.domain.Role;
import com.plateforme.scolaire.domain.Utilisateur;
import com.plateforme.scolaire.domain.ZoneGeographique;
import com.plateforme.scolaire.repository.AffectationRoleRepository;
import com.plateforme.scolaire.repository.ClasseRepository;
import com.plateforme.scolaire.repository.CycleScolaireRepository;
import com.plateforme.scolaire.repository.EtablissementRepository;
import com.plateforme.scolaire.repository.InscriptionRepository;
import com.plateforme.scolaire.repository.UtilisateurRepository;
import com.plateforme.scolaire.repository.ZoneGeographiqueRepository;
import com.plateforme.scolaire.web.dto.ComptePersonnelDto;
import com.plateforme.scolaire.web.dto.DirectionRequest;
import com.plateforme.scolaire.web.dto.EtablissementDto;
import com.plateforme.scolaire.web.dto.EtablissementRequest;
import com.plateforme.scolaire.web.dto.SyntheseNationaleDto;
import com.plateforme.scolaire.web.dto.ZoneDto;
import com.plateforme.scolaire.web.dto.ZoneRequest;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Administration nationale (Lot 6) : zones, établissements, comptes de direction, chiffres clés.
 * Réservé au ministère : l'autorisation (@PreAuthorize) est posée sur le contrôleur.
 * Le ministère crée la structure ; il n'écrit jamais dans les données internes d'un établissement.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AdministrationService {

    private final ZoneGeographiqueRepository zoneRepository;
    private final EtablissementRepository etablissementRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final AffectationRoleRepository affectationRoleRepository;
    private final ClasseRepository classeRepository;
    private final InscriptionRepository inscriptionRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final CycleScolaireRepository cycleRepository;

    // ---------- Zones ----------

    @Transactional(readOnly = true)
    public List<ZoneDto> zones() {
        return zoneRepository.findAllByOrderByNom().stream().map(ZoneDto::de).toList();
    }

    public ZoneDto creerZone(ZoneRequest r) {
        ZoneGeographique zone = new ZoneGeographique();
        if (r.parentId() != null) {
            zone.setParent(zoneRepository.findById(r.parentId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Zone parente inconnue")));
        }
        zone.setNom(r.nom().trim());
        zone.setTypeZone(r.typeZone().trim());
        ZoneGeographique enregistree = zoneRepository.save(zone);
        auditService.enregistrer("CREATION_ZONE", "zone_geographique", enregistree.getId(),
                Map.of("nom", enregistree.getNom()));
        return ZoneDto.de(enregistree);
    }

    /** Renomme une zone ou la déplace dans la hiérarchie, sans jamais créer de cycle. */
    public ZoneDto modifierZone(Long id, ZoneRequest r) {
        ZoneGeographique zone = chargerZone(id);
        ZoneGeographique parent = null;
        if (r.parentId() != null) {
            parent = zoneRepository.findById(r.parentId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Zone parente inconnue"));
            verifierSansCycle(zone, parent);
        }
        zone.setParent(parent);
        zone.setNom(r.nom().trim());
        zone.setTypeZone(r.typeZone().trim());
        auditService.enregistrer("MODIFICATION_ZONE", "zone_geographique", id, Map.of("nom", zone.getNom()));
        return ZoneDto.de(zone);
    }

    /** Une zone qui a des sous-zones, des établissements ou une direction régionale ne se supprime pas. */
    public void supprimerZone(Long id) {
        ZoneGeographique zone = chargerZone(id);
        if (zoneRepository.existsByParentId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette zone contient des sous-zones");
        }
        if (etablissementRepository.existsByZoneId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Des établissements sont rattachés à cette zone");
        }
        if (affectationRoleRepository.existsByZoneId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Un compte de direction régionale dépend de cette zone");
        }
        zoneRepository.delete(zone);
        auditService.enregistrer("SUPPRESSION_ZONE", "zone_geographique", id, Map.of("nom", zone.getNom()));
    }

    // ---------- Établissements ----------

    public EtablissementDto creerEtablissement(EtablissementRequest r) {
        String code = r.codeOfficiel().trim().toUpperCase();
        if (etablissementRepository.existsByCodeOfficiel(code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce code officiel est déjà utilisé");
        }
        Etablissement etablissement = new Etablissement();
        etablissement.setCodeOfficiel(code);
        appliquer(etablissement, r);
        Etablissement enregistre = etablissementRepository.save(etablissement);
        auditService.enregistrer("CREATION_ETABLISSEMENT", "etablissement", enregistre.getId(),
                Map.of("code", code, "nom", enregistre.getNom()));
        return EtablissementDto.de(enregistre);
    }

    /** Le code officiel est un identifiant : il ne change pas après la création. */
    public EtablissementDto modifierEtablissement(Long id, EtablissementRequest r) {
        Etablissement etablissement = charger(id);
        appliquer(etablissement, r);
        auditService.enregistrer("MODIFICATION_ETABLISSEMENT", "etablissement", id,
                Map.of("nom", etablissement.getNom()));
        return EtablissementDto.de(etablissement);
    }

    public EtablissementDto changerStatut(Long id, String statut) {
        Etablissement etablissement = charger(id);
        String ancien = etablissement.getStatut();
        etablissement.setStatut(statut);
        auditService.enregistrer("CHANGEMENT_STATUT_ETABLISSEMENT", "etablissement", id,
                Map.of("avant", ancien, "apres", statut));
        return EtablissementDto.de(etablissement);
    }

    /** Crée le compte de direction : il devra changer son mot de passe à la première connexion. */
    public ComptePersonnelDto creerDirection(Long etablissementId, DirectionRequest r) {
        Etablissement etablissement = charger(etablissementId);
        if (!"ACTIF".equals(etablissement.getStatut())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "L'établissement n'est pas actif");
        }
        Utilisateur utilisateur = creerCompte(r);

        AffectationRole role = new AffectationRole();
        role.setUtilisateur(utilisateur);
        role.setRole(Role.DIRECTION_ETABLISSEMENT);
        role.setPerimetre(Perimetre.ETABLISSEMENT);
        role.setEtablissement(etablissement);
        affectationRoleRepository.save(role);

        auditService.enregistrer("CREATION_COMPTE_DIRECTION", "utilisateur", utilisateur.getId(),
                Map.of("etablissementId", etablissementId, "email", utilisateur.getEmail()));
        return ComptePersonnelDto.de(utilisateur, Role.DIRECTION_ETABLISSEMENT.name());
    }

    /** Compte de direction régionale : lecture seule sur toutes les écoles de la zone et de ses sous-zones. */
    public ComptePersonnelDto creerDirectionRegionale(Long zoneId, DirectionRequest r) {
        ZoneGeographique zone = zoneRepository.findById(zoneId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Zone introuvable"));
        Utilisateur utilisateur = creerCompte(r);

        AffectationRole role = new AffectationRole();
        role.setUtilisateur(utilisateur);
        role.setRole(Role.DIRECTION_REGIONALE);
        role.setPerimetre(Perimetre.ZONE);
        role.setZone(zone);
        affectationRoleRepository.save(role);

        auditService.enregistrer("CREATION_COMPTE_DIRECTION_REGIONALE", "utilisateur", utilisateur.getId(),
                Map.of("zoneId", zoneId, "email", utilisateur.getEmail()));
        return ComptePersonnelDto.de(utilisateur, Role.DIRECTION_REGIONALE.name());
    }

    // ---------- Chiffres clés ----------

    @Transactional(readOnly = true)
    public SyntheseNationaleDto synthese(Long anneeScolaireId) {
        return new SyntheseNationaleDto(anneeScolaireId,
                etablissementRepository.count(),
                etablissementRepository.countByStatut("ACTIF"),
                etablissementRepository.countByStatut("SUSPENDU"),
                etablissementRepository.countByStatut("FERME"),
                classeRepository.countByAnneeScolaireId(anneeScolaireId),
                inscriptionRepository.countByAnneeScolaireIdAndStatut(anneeScolaireId, "ACCEPTEE"));
    }

    // ---------- Interne ----------

    /** Compte à mot de passe initial : l'utilisateur devra le changer à sa première connexion. */
    private Utilisateur creerCompte(DirectionRequest r) {
        String email = r.email().trim();
        if (utilisateurRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette adresse e-mail est déjà utilisée");
        }
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setEmail(email);
        utilisateur.setNom(r.nom().trim());
        utilisateur.setPrenom(r.prenom().trim());
        utilisateur.setTelephone(r.telephone());
        utilisateur.setMotDePasseHash(passwordEncoder.encode(r.motDePasseInitial()));
        utilisateur.setDoitChangerMotDePasse(true);
        utilisateurRepository.save(utilisateur);
        return utilisateur;
    }

    private ZoneGeographique chargerZone(Long id) {
        return zoneRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Zone introuvable"));
    }

    /** Remonte depuis le futur parent : si on retombe sur la zone elle-même, on créerait une boucle. */
    private static void verifierSansCycle(ZoneGeographique zone, ZoneGeographique futurParent) {
        java.util.Set<Long> vus = new java.util.HashSet<>();
        for (ZoneGeographique courant = futurParent; courant != null; courant = courant.getParent()) {
            if (courant.getId().equals(zone.getId()) || !vus.add(courant.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Une zone ne peut pas être placée sous elle-même ni sous l'une de ses sous-zones");
            }
        }
    }

    private Etablissement charger(Long id) {
        return etablissementRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Établissement introuvable"));
    }

    private void appliquer(Etablissement e, EtablissementRequest r) {
        ZoneGeographique zone = r.zoneId() == null ? null : zoneRepository.findById(r.zoneId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Zone inconnue"));
        e.setNom(r.nom().trim());
        e.setZone(zone);
        e.setSecteur(r.secteur());
        var ids = new java.util.LinkedHashSet<>(r.cycleIds());
        var cycles = cycleRepository.findAllById(ids);
        if (cycles.size() != ids.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cycle inconnu");
        }
        e.setCycles(new java.util.LinkedHashSet<>(cycles));
        e.setAdresse(r.adresse());
        e.setCapacite(r.capacite());
    }
}
