package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.AffectationRole;
import com.plateforme.scolaire.domain.Etablissement;
import com.plateforme.scolaire.domain.Perimetre;
import com.plateforme.scolaire.domain.Role;
import com.plateforme.scolaire.domain.Utilisateur;
import com.plateforme.scolaire.repository.AffectationRoleRepository;
import com.plateforme.scolaire.repository.EleveRepository;
import com.plateforme.scolaire.repository.EtablissementRepository;
import com.plateforme.scolaire.repository.LienParentalRepository;
import com.plateforme.scolaire.repository.UtilisateurRepository;
import com.plateforme.scolaire.security.PerimetreComptesService;
import com.plateforme.scolaire.web.dto.ComptePersonnelDto;
import com.plateforme.scolaire.web.dto.CompteDto;
import com.plateforme.scolaire.web.dto.PersonnelRequest;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Administration des comptes : liste, création du personnel administratif et réinitialisation de mot de passe.
 * L'autorisation est posée sur le contrôleur ({@code perimetreComptes}).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ComptesService {

    /** En dessous de ce nombre de caractères, on ne liste pas les parents et élèves (trop nombreux). */
    static final int MIN_RECHERCHE_FAMILLES = 2;
    static final int MAX_PAR_CATEGORIE = 50;

    private static final Set<Role> ROLES_PERSONNEL =
            EnumSet.of(Role.DIRECTION_ETABLISSEMENT, Role.PERSONNEL_ADMIN, Role.ENSEIGNANT);
    private static final Set<Role> ROLES_VUS_PAR_LE_MINISTERE = EnumSet.of(Role.DIRECTION_ETABLISSEMENT);

    private final EtablissementRepository etablissementRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final AffectationRoleRepository affectationRoleRepository;
    private final LienParentalRepository lienParentalRepository;
    private final EleveRepository eleveRepository;
    private final PerimetreComptesService perimetreComptes;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final Clock clock;

    /**
     * La direction voit son personnel, ses enseignants et, à partir de deux caractères de recherche,
     * les parents et élèves inscrits. Le ministère ne voit que les directions.
     */
    @Transactional(readOnly = true)
    public List<CompteDto> lister(Long etablissementId, String recherche) {
        String q = recherche == null ? "" : recherche.trim();
        boolean direction = perimetreComptes.estDirectionDe(etablissementId);
        List<CompteDto> comptes = new ArrayList<>();

        for (AffectationRole a : affectationRoleRepository.rechercherComptes(
                etablissementId, direction ? ROLES_PERSONNEL : ROLES_VUS_PAR_LE_MINISTERE, q)) {
            comptes.add(CompteDto.de(a.getUtilisateur(), a.getRole().name()));
        }
        if (direction && q.length() >= MIN_RECHERCHE_FAMILLES) {
            lienParentalRepository.rechercherParents(etablissementId, q).stream().limit(MAX_PAR_CATEGORIE)
                    .forEach(u -> comptes.add(CompteDto.de(u, Role.PARENT.name())));
            eleveRepository.rechercherComptesEleves(etablissementId, q).stream().limit(MAX_PAR_CATEGORIE)
                    .forEach(u -> comptes.add(CompteDto.de(u, Role.ELEVE.name())));
        }
        return comptes;
    }

    public ComptePersonnelDto creerPersonnel(Long etablissementId, PersonnelRequest r) {
        Etablissement etablissement = etablissementRepository.findById(etablissementId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Établissement introuvable"));
        if (!"ACTIF".equals(etablissement.getStatut())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "L'établissement n'est pas actif");
        }
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

        AffectationRole role = new AffectationRole();
        role.setUtilisateur(utilisateur);
        role.setRole(Role.PERSONNEL_ADMIN);
        role.setPerimetre(Perimetre.ETABLISSEMENT);
        role.setEtablissement(etablissement);
        affectationRoleRepository.save(role);

        auditService.enregistrer("CREATION_COMPTE_PERSONNEL", "utilisateur", utilisateur.getId(),
                Map.of("etablissementId", etablissementId, "email", utilisateur.getEmail()));
        return ComptePersonnelDto.de(utilisateur, Role.PERSONNEL_ADMIN.name());
    }

    /**
     * Fixe un mot de passe temporaire. Les jetons déjà émis sont invalidés, le verrouillage est levé et la
     * personne devra choisir un nouveau mot de passe à sa prochaine connexion. Le mot de passe n'est jamais
     * écrit dans le journal.
     */
    public void reinitialiserMotDePasse(Long utilisateurId, String motDePasseTemporaire) {
        Utilisateur utilisateur = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
        utilisateur.setMotDePasseHash(passwordEncoder.encode(motDePasseTemporaire));
        utilisateur.setDoitChangerMotDePasse(true);
        utilisateur.setMotDePasseModifieLe(Instant.now(clock));
        utilisateur.setEchecsConnexion(0);
        utilisateur.setVerrouilleJusquA(null);
        auditService.enregistrer("REINITIALISATION_MOT_DE_PASSE", "utilisateur", utilisateurId,
                Map.of("email", utilisateur.getEmail()));
    }
}
