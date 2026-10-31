package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.AffectationRole;
import com.plateforme.scolaire.domain.Enseignant;
import com.plateforme.scolaire.domain.Etablissement;
import com.plateforme.scolaire.domain.Role;
import com.plateforme.scolaire.repository.AffectationEnseignantRepository;
import com.plateforme.scolaire.repository.AffectationRoleRepository;
import com.plateforme.scolaire.repository.EnseignantRepository;
import com.plateforme.scolaire.repository.EtablissementRepository;
import com.plateforme.scolaire.security.PerimetreService;
import com.plateforme.scolaire.web.dto.LigneEtablissementStatDto;
import com.plateforme.scolaire.web.dto.MutationRequest;
import com.plateforme.scolaire.web.dto.RapportPostesDto;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Postes d'enseignants, besoins par établissement et mutation d'un enseignant d'un établissement public à un autre
 * établissement public (les enseignants du privé ne sont pas mutés par l'État).
 */
@Service
@Transactional
public class PostesService {

    private final StatistiquesService statistiques;
    private final EtablissementRepository etablissementRepository;
    private final EnseignantRepository enseignantRepository;
    private final AffectationRoleRepository affectationRoleRepository;
    private final AffectationEnseignantRepository affectationEnseignantRepository;
    private final PerimetreService perimetreService;
    private final AuditService auditService;
    private final int norme;

    public PostesService(StatistiquesService statistiques, EtablissementRepository etablissementRepository,
                         EnseignantRepository enseignantRepository, AffectationRoleRepository affectationRoleRepository,
                         AffectationEnseignantRepository affectationEnseignantRepository,
                         PerimetreService perimetreService, AuditService auditService,
                         @Value("${app.postes.eleves-par-enseignant:40}") int norme) {
        this.statistiques = statistiques;
        this.etablissementRepository = etablissementRepository;
        this.enseignantRepository = enseignantRepository;
        this.affectationRoleRepository = affectationRoleRepository;
        this.affectationEnseignantRepository = affectationEnseignantRepository;
        this.perimetreService = perimetreService;
        this.auditService = auditService;
        this.norme = Math.max(1, norme);
    }

    @Transactional(readOnly = true)
    public RapportPostesDto nationale(Long anneeId, Long zoneId) {
        return rapport(statistiques.lignesNationales(anneeId, zoneId));
    }

    @Transactional(readOnly = true)
    public RapportPostesDto regionale(Long anneeId) {
        return rapport(statistiques.lignesRegionales(anneeId));
    }

    private RapportPostesDto rapport(List<LigneEtablissementStatDto> stats) {
        Map<Long, Integer> postes = new HashMap<>();
        etablissementRepository.findAllById(stats.stream().map(LigneEtablissementStatDto::id).toList())
                .forEach(e -> postes.put(e.getId(), e.getPostesEnseignants()));
        List<RapportPostesDto.Ligne> lignes = stats.stream()
                .map(s -> ligne(s, postes.get(s.id()), norme))
                .sorted((a, b) -> Integer.compare(b.besoinEstime(), a.besoinEstime()))
                .toList();
        return new RapportPostesDto(norme, totaux(lignes), lignes);
    }

    static RapportPostesDto.Ligne ligne(LigneEtablissementStatDto s, Integer postes, int norme) {
        long enseignants = s.enseignants();
        Integer vacants = postes == null ? null : (int) Math.max(postes - enseignants, 0);
        int surnombre = postes == null ? 0 : (int) Math.max(enseignants - postes, 0);
        long necessaires = (s.eleves() + norme - 1) / norme;
        int besoin = (int) Math.max(necessaires - enseignants, 0);
        BigDecimal ratio = enseignants == 0 ? null
                : BigDecimal.valueOf(s.eleves()).divide(BigDecimal.valueOf(enseignants), 1, RoundingMode.HALF_UP);
        return new RapportPostesDto.Ligne(s.id(), s.codeOfficiel(), s.nom(), s.secteur(), s.zoneId(), s.zone(),
                s.eleves(), enseignants, postes, vacants, surnombre, besoin, ratio);
    }

    static RapportPostesDto.Totaux totaux(List<RapportPostesDto.Ligne> l) {
        return new RapportPostesDto.Totaux(l.size(),
                l.stream().mapToLong(RapportPostesDto.Ligne::eleves).sum(),
                l.stream().mapToLong(RapportPostesDto.Ligne::enseignants).sum(),
                l.stream().filter(x -> x.postes() != null).mapToLong(RapportPostesDto.Ligne::postes).sum(),
                l.stream().filter(x -> x.vacants() != null).mapToLong(RapportPostesDto.Ligne::vacants).sum(),
                l.stream().mapToLong(RapportPostesDto.Ligne::besoinEstime).sum(),
                l.stream().mapToLong(RapportPostesDto.Ligne::enSurnombre).sum());
    }

    /** L'accès à l'établissement est contrôlé par le contrôleur. */
    public void definirPostes(Long etablissementId, Integer postes) {
        Etablissement e = etablissementRepository.findById(etablissementId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Établissement introuvable"));
        e.setPostesEnseignants(postes);
        auditService.enregistrer("POSTES_ENSEIGNANTS_MODIFIES", "Etablissement", e.getId(),
                Map.of("etablissement", e.getNom(), "postes", postes == null ? "non renseigné" : postes));
    }

    /**
     * Mute un enseignant d'un établissement vers un autre, tous deux dans le périmètre de l'utilisateur. Ses
     * affectations aux classes de l'ancien établissement sont retirées (les notes déjà saisies restent
     * attachées aux classes et aux élèves).
     */
    static void verifierPublics(Etablissement origine, Etablissement vers) {
        if (!"PUBLIC".equals(origine.getSecteur()) || !"PUBLIC".equals(vers.getSecteur())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Une mutation ne peut se faire qu'entre deux établissements publics");
        }
    }

    public void muter(Long enseignantId, MutationRequest r) {
        if (r.depuisEtablissementId().equals(r.versEtablissementId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choisissez un autre établissement");
        }
        if (!perimetreService.peutAccederEtablissement(r.depuisEtablissementId())
                || !perimetreService.peutAccederEtablissement(r.versEtablissementId())) {
            throw new AccessDeniedException("Établissement hors de votre périmètre");
        }
        Enseignant enseignant = enseignantRepository.findById(enseignantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Enseignant introuvable"));
        Etablissement vers = etablissementRepository.findById(r.versEtablissementId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Établissement d'accueil introuvable"));
        Etablissement origine = etablissementRepository.findById(r.depuisEtablissementId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Établissement d'origine introuvable"));
        verifierPublics(origine, vers);
        if (!"ACTIF".equals(vers.getStatut())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "L'établissement d'accueil n'est pas actif");
        }
        Long compte = enseignant.getUtilisateur().getId();
        AffectationRole role = affectationRoleRepository
                .findByUtilisateurIdAndRoleAndEtablissementId(compte, Role.ENSEIGNANT, r.depuisEtablissementId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Cet enseignant ne travaille pas dans l'établissement d'origine"));
        if (affectationRoleRepository
                .findByUtilisateurIdAndRoleAndEtablissementId(compte, Role.ENSEIGNANT, r.versEtablissementId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cet enseignant travaille déjà dans l'établissement d'accueil");
        }
        var aRetirer = affectationEnseignantRepository.findByEnseignantUtilisateurId(compte).stream()
                .filter(a -> a.getClasse().getEtablissement().getId().equals(r.depuisEtablissementId())).toList();
        affectationEnseignantRepository.deleteAll(aRetirer);
        String depuis = role.getEtablissement().getNom();
        role.setEtablissement(vers);
        auditService.enregistrer("ENSEIGNANT_MUTE", "Enseignant", enseignant.getId(),
                Map.of("de", depuis, "vers", vers.getNom(), "classesRetirees", aRetirer.size()));
    }
}
