package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.AffectationRole;
import com.plateforme.scolaire.domain.Eleve;
import com.plateforme.scolaire.domain.LienParental;
import com.plateforme.scolaire.domain.Perimetre;
import com.plateforme.scolaire.domain.Role;
import com.plateforme.scolaire.domain.Utilisateur;
import com.plateforme.scolaire.repository.AffectationRoleRepository;
import com.plateforme.scolaire.repository.EleveRepository;
import com.plateforme.scolaire.repository.InscriptionRepository;
import com.plateforme.scolaire.repository.LienParentalRepository;
import com.plateforme.scolaire.repository.UtilisateurRepository;
import com.plateforme.scolaire.web.dto.EnfantDto;
import com.plateforme.scolaire.web.dto.LienParentalDto;
import com.plateforme.scolaire.web.dto.ParentRequest;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class ParentService {

    private final EleveRepository eleveRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final AffectationRoleRepository affectationRoleRepository;
    private final LienParentalRepository lienParentalRepository;
    private final InscriptionRepository inscriptionRepository;
    private final PasswordEncoder passwordEncoder;

    /** Rattache un parent à un élève, en créant son compte si besoin. */
    public LienParentalDto lier(Long eleveId, ParentRequest r) {
        Eleve eleve = eleveRepository.findById(eleveId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Élève introuvable"));
        String email = r.email().trim();

        Optional<Utilisateur> existant = utilisateurRepository.findByEmailIgnoreCase(email);
        Utilisateur parent;
        if (existant.isPresent()) {
            parent = existant.get();
            if (!affectationRoleRepository.existsByUtilisateurIdAndRole(parent.getId(), Role.PARENT)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Cette adresse e-mail est déjà utilisée par un autre type de compte");
            }
            if (lienParentalRepository.existsByEleveIdAndUtilisateurId(eleve.getId(), parent.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce parent est déjà lié à cet élève");
            }
        } else {
            if (r.motDePasseInitial() == null || r.motDePasseInitial().length() < 10) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Un mot de passe initial d'au moins 10 caractères est requis pour créer le compte");
            }
            parent = new Utilisateur();
            parent.setEmail(email);
            parent.setNom(r.nom().trim());
            parent.setPrenom(r.prenom().trim());
            parent.setTelephone(r.telephone());
            parent.setMotDePasseHash(passwordEncoder.encode(r.motDePasseInitial()));
        parent.setDoitChangerMotDePasse(true);
            utilisateurRepository.save(parent);

            AffectationRole role = new AffectationRole();
            role.setUtilisateur(parent);
            role.setRole(Role.PARENT);
            role.setPerimetre(Perimetre.PERSONNEL);
            affectationRoleRepository.save(role);
        }

        LienParental lien = new LienParental();
        lien.setEleve(eleve);
        lien.setUtilisateur(parent);
        lien.setRelation(r.relation().trim());
        lien.setResponsableLegal(Boolean.TRUE.equals(r.responsableLegal()));
        return LienParentalDto.de(lienParentalRepository.save(lien));
    }

    /** Les enfants rattachés au parent connecté, avec leur classe actuelle. */
    @Transactional(readOnly = true)
    public List<EnfantDto> mesEnfants(Long utilisateurId) {
        return lienParentalRepository.findByUtilisateurId(utilisateurId).stream()
                .map(lien -> EnfantDto.de(lien, inscriptionRepository
                        .findByEleveIdAndStatutOrderByAnneeScolaireDateDebutDesc(lien.getEleve().getId(), "ACCEPTEE")
                        .stream().findFirst().orElse(null)))
                .sorted(Comparator.comparing(EnfantDto::nom).thenComparing(EnfantDto::prenom))
                .toList();
    }

    /** Parents rattachés à un élève (réservé à ceux qui gèrent l'élève : direction de son établissement). */
    @Transactional(readOnly = true)
    public List<LienParentalDto> parentsDeLEleve(Long eleveId) {
        if (!eleveRepository.existsById(eleveId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Élève introuvable");
        }
        return lienParentalRepository.findByEleveId(eleveId).stream().map(LienParentalDto::de).toList();
    }
}
