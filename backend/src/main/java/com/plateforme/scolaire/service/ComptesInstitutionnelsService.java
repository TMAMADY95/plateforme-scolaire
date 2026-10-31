package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.AffectationRole;
import com.plateforme.scolaire.domain.Perimetre;
import com.plateforme.scolaire.domain.Role;
import com.plateforme.scolaire.domain.Utilisateur;
import com.plateforme.scolaire.repository.AffectationRoleRepository;
import com.plateforme.scolaire.repository.UtilisateurRepository;
import com.plateforme.scolaire.security.PerimetreService;
import com.plateforme.scolaire.security.UtilisateurPrincipal;
import com.plateforme.scolaire.web.dto.CompteInstitutionnelDto;
import com.plateforme.scolaire.web.dto.DirectionRequest;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Gestion par le ministère des comptes du ministère et des directions régionales : liste, création d'un compte
 * ministère, activation et désactivation. La création des comptes régionaux reste liée à une zone
 * (AdministrationService.creerDirectionRegionale) ; la réinitialisation passe par ComptesService.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ComptesInstitutionnelsService {

    private static final EnumSet<Role> ROLES = EnumSet.of(Role.MINISTERE, Role.DIRECTION_REGIONALE);

    private final AffectationRoleRepository affectationRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final PerimetreService perimetreService;
    private final ComptesService comptesService;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<CompteInstitutionnelDto> lister() {
        return affectationRepository.destinatairesTous(ROLES).stream()
                .sorted(Comparator.comparing((AffectationRole a) -> a.getRole().name())
                        .thenComparing(a -> a.getUtilisateur().getNom(), String.CASE_INSENSITIVE_ORDER))
                .map(CompteInstitutionnelDto::de).toList();
    }

    public CompteInstitutionnelDto creerMinistere(DirectionRequest r) {
        String email = r.email().trim();
        if (utilisateurRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette adresse e-mail est déjà utilisée");
        }
        Utilisateur u = new Utilisateur();
        u.setEmail(email);
        u.setNom(r.nom().trim());
        u.setPrenom(r.prenom().trim());
        u.setTelephone(r.telephone());
        u.setMotDePasseHash(passwordEncoder.encode(r.motDePasseInitial()));
        u.setDoitChangerMotDePasse(true);
        utilisateurRepository.save(u);

        AffectationRole a = new AffectationRole();
        a.setUtilisateur(u);
        a.setRole(Role.MINISTERE);
        a.setPerimetre(Perimetre.NATIONAL);
        affectationRepository.save(a);
        auditService.enregistrer("CREATION_COMPTE_MINISTERE", "utilisateur", u.getId(), Map.of("email", email));
        return CompteInstitutionnelDto.de(a);
    }

    /** On ne désactive jamais son propre compte : le ministère ne doit pas pouvoir s'enfermer dehors. */
    public void activer(Long utilisateurId, boolean actif) {
        verifierInstitutionnel(utilisateurId);
        UtilisateurPrincipal moi = perimetreService.utilisateurCourant();
        if (!actif && moi != null && moi.getId().equals(utilisateurId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Vous ne pouvez pas désactiver votre propre compte");
        }
        Utilisateur u = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Compte introuvable"));
        u.setActif(actif);
        auditService.enregistrer(actif ? "COMPTE_ACTIVE" : "COMPTE_DESACTIVE", "utilisateur", utilisateurId,
                Map.of("email", u.getEmail()));
    }

    public void reinitialiser(Long utilisateurId, String motDePasseTemporaire) {
        verifierInstitutionnel(utilisateurId);
        comptesService.reinitialiserMotDePasse(utilisateurId, motDePasseTemporaire);
    }

    private void verifierInstitutionnel(Long utilisateurId) {
        boolean institutionnel = affectationRepository.findByUtilisateurId(utilisateurId).stream()
                .anyMatch(a -> ROLES.contains(a.getRole()));
        if (!institutionnel) {
            throw new AccessDeniedException("Ce compte n'est ni du ministère ni d'une direction régionale");
        }
    }
}
