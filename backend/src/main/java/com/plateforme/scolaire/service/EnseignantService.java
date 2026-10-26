package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.AffectationEnseignant;
import com.plateforme.scolaire.domain.AffectationRole;
import com.plateforme.scolaire.domain.Classe;
import com.plateforme.scolaire.domain.Enseignant;
import com.plateforme.scolaire.domain.Etablissement;
import com.plateforme.scolaire.domain.Matiere;
import com.plateforme.scolaire.domain.Perimetre;
import com.plateforme.scolaire.domain.Role;
import com.plateforme.scolaire.domain.Utilisateur;
import com.plateforme.scolaire.repository.AffectationEnseignantRepository;
import com.plateforme.scolaire.repository.AffectationRoleRepository;
import com.plateforme.scolaire.repository.ClasseRepository;
import com.plateforme.scolaire.repository.EnseignantRepository;
import com.plateforme.scolaire.repository.EtablissementRepository;
import com.plateforme.scolaire.repository.MatiereRepository;
import com.plateforme.scolaire.repository.UtilisateurRepository;
import com.plateforme.scolaire.web.dto.AffectationEnseignantDto;
import com.plateforme.scolaire.web.dto.AffectationEnseignantRequest;
import com.plateforme.scolaire.web.dto.EnseignantDto;
import com.plateforme.scolaire.web.dto.EnseignantRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class EnseignantService {

    private final EnseignantRepository enseignantRepository;
    private final AffectationEnseignantRepository affectationRepository;
    private final AffectationRoleRepository affectationRoleRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final EtablissementRepository etablissementRepository;
    private final ClasseRepository classeRepository;
    private final MatiereRepository matiereRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<EnseignantDto> lister(Long etablissementId) {
        if (!etablissementRepository.existsById(etablissementId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Établissement introuvable");
        }
        return enseignantRepository.findByEtablissement(etablissementId, Role.ENSEIGNANT).stream()
                .map(EnseignantDto::de).toList();
    }

    /** Crée le compte de connexion, le rôle ENSEIGNANT dans cet établissement et la fiche enseignant. */
    public EnseignantDto creer(Long etablissementId, EnseignantRequest r) {
        Etablissement etablissement = etablissementRepository.findById(etablissementId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Établissement introuvable"));
        if (!"ACTIF".equals(etablissement.getStatut())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "L'établissement n'est pas actif");
        }
        String email = r.email().trim();
        if (utilisateurRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette adresse e-mail est déjà utilisée");
        }
        String matricule = r.matricule() == null || r.matricule().isBlank() ? null : r.matricule().trim();
        if (matricule != null && enseignantRepository.existsByMatricule(matricule)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce matricule est déjà utilisé");
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
        role.setRole(Role.ENSEIGNANT);
        role.setPerimetre(Perimetre.ETABLISSEMENT);
        role.setEtablissement(etablissement);
        affectationRoleRepository.save(role);

        Enseignant enseignant = new Enseignant();
        enseignant.setUtilisateur(utilisateur);
        enseignant.setMatricule(matricule);
        return EnseignantDto.de(enseignantRepository.save(enseignant));
    }

    @Transactional(readOnly = true)
    public List<AffectationEnseignantDto> affectations(Long classeId) {
        if (!classeRepository.existsById(classeId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Classe introuvable");
        }
        return affectationRepository.findByClasseId(classeId).stream().map(AffectationEnseignantDto::de).toList();
    }

    /** Affecte un enseignant à une classe pour une matière. */
    public AffectationEnseignantDto affecter(Long classeId, AffectationEnseignantRequest r) {
        Classe classe = classeRepository.findById(classeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Classe introuvable"));
        Enseignant enseignant = enseignantRepository.findById(r.enseignantId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enseignant inconnu"));
        Matiere matiere = matiereRepository.findById(r.matiereId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Matière inconnue"));
        long rattachements = enseignantRepository.compterRattachements(
                enseignant.getId(), classe.getEtablissement().getId(), Role.ENSEIGNANT);
        if (rattachements == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cet enseignant n'est pas rattaché à l'établissement de la classe");
        }
        if (affectationRepository.existsByEnseignantIdAndClasseIdAndMatiereId(
                enseignant.getId(), classe.getId(), matiere.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette affectation existe déjà");
        }
        AffectationEnseignant affectation = new AffectationEnseignant();
        affectation.setClasse(classe);
        affectation.setEnseignant(enseignant);
        affectation.setMatiere(matiere);
        return AffectationEnseignantDto.de(affectationRepository.save(affectation));
    }
}
