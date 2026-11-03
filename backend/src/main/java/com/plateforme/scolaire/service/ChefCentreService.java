package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.AccesCentre;
import com.plateforme.scolaire.domain.AffectationRole;
import com.plateforme.scolaire.domain.CentreExamen;
import com.plateforme.scolaire.domain.Perimetre;
import com.plateforme.scolaire.domain.Role;
import com.plateforme.scolaire.domain.Utilisateur;
import com.plateforme.scolaire.repository.AccesCentreRepository;
import com.plateforme.scolaire.repository.AffectationRoleRepository;
import com.plateforme.scolaire.repository.CentreExamenRepository;
import com.plateforme.scolaire.repository.UtilisateurRepository;
import com.plateforme.scolaire.security.PerimetreService;
import com.plateforme.scolaire.web.dto.ChefCentreDto;
import com.plateforme.scolaire.web.dto.DirectionRequest;
import com.plateforme.scolaire.web.dto.MonCentreDto;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Comptes des chefs de centre : créés par la DGECS, l'IRE ou la direction préfectorale/communale dont dépend le centre. Le
 * chef de centre n'a accès qu'aux centres qui lui sont confiés (personnel, incidents, déroulement des épreuves).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ChefCentreService {

    private final CentreExamenRepository centres;
    private final AccesCentreRepository acces;
    private final UtilisateurRepository utilisateurs;
    private final AffectationRoleRepository affectations;
    private final PasswordEncoder encodeur;
    private final PerimetreService perimetreService;
    private final AuditService audit;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<ChefCentreDto> chefs(Long examenId, Long centreId) {
        centre(examenId, centreId);
        return acces.duCentre(centreId).stream().map(a -> dto(a.getUtilisateur())).toList();
    }

    /**
     * Confie le centre à un chef. Si l'adresse e-mail est déjà celle d'un chef de centre, le compte est réutilisé (un chef peut
     * tenir plusieurs centres) ; sinon un compte est créé avec le mot de passe initial, à changer à la première connexion.
     */
    public List<ChefCentreDto> designer(Long examenId, Long centreId, DirectionRequest r) {
        CentreExamen centre = centre(examenId, centreId);
        String email = r.email().trim();
        Utilisateur u = utilisateurs.findByEmailIgnoreCase(email).orElse(null);
        if (u == null) {
            u = new Utilisateur();
            u.setEmail(email);
            u.setNom(r.nom().trim());
            u.setPrenom(r.prenom().trim());
            u.setTelephone(r.telephone());
            u.setMotDePasseHash(encodeur.encode(r.motDePasseInitial()));
            u.setDoitChangerMotDePasse(true);
            utilisateurs.save(u);
            AffectationRole role = new AffectationRole();
            role.setUtilisateur(u);
            role.setRole(Role.CHEF_CENTRE);
            role.setPerimetre(Perimetre.PERSONNEL);
            affectations.save(role);
        } else if (affectations.findByUtilisateurId(u.getId()).stream().anyMatch(a -> a.getRole() != Role.CHEF_CENTRE)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cette adresse e-mail est déjà utilisée par un compte qui n'est pas un chef de centre");
        }
        if (acces.existsByUtilisateurIdAndCentreId(u.getId(), centreId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce chef de centre est déjà désigné pour ce centre");
        }
        AccesCentre a = new AccesCentre();
        a.setUtilisateur(u);
        a.setCentre(centre);
        a.setCreeLe(Instant.now(clock));
        acces.save(a);
        audit.enregistrer("CHEF_CENTRE_DESIGNE", "CentreExamen", centreId, Map.of("email", email));
        return chefs(examenId, centreId);
    }

    /** Retire l'accès au centre ; le compte reste (il peut tenir d'autres centres) mais n'a plus rien à y faire. */
    public List<ChefCentreDto> retirer(Long examenId, Long centreId, Long utilisateurId) {
        centre(examenId, centreId);
        AccesCentre a = acces.findByUtilisateurIdAndCentreId(utilisateurId, centreId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ce chef n'est pas désigné pour ce centre"));
        acces.delete(a);
        audit.enregistrer("CHEF_CENTRE_RETIRE", "CentreExamen", centreId, Map.of("utilisateur", utilisateurId));
        return chefs(examenId, centreId);
    }

    @Transactional(readOnly = true)
    public List<MonCentreDto> mesCentres() {
        var p = perimetreService.utilisateurCourant();
        if (p == null) {
            return List.of();
        }
        return acces.deLUtilisateur(p.getId()).stream().map(a -> {
            CentreExamen c = a.getCentre();
            return new MonCentreDto(c.getExamen().getId(), c.getExamen().getLibelle(), c.getExamen().getStatutResultats(),
                    c.getId(), c.getNom(), c.getZone() == null ? null : c.getZone().getNom());
        }).toList();
    }

    private ChefCentreDto dto(Utilisateur u) {
        return new ChefCentreDto(u.getId(), u.getNom(), u.getPrenom(), u.getEmail(), u.isActif());
    }

    private CentreExamen centre(Long examenId, Long centreId) {
        return centres.findById(centreId).filter(c -> c.getExamen().getId().equals(examenId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Centre inconnu pour cet examen"));
    }
}
