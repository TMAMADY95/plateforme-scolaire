package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.AffectationEnseignant;
import com.plateforme.scolaire.domain.RendezVous;
import com.plateforme.scolaire.repository.AffectationEnseignantRepository;
import com.plateforme.scolaire.repository.EleveRepository;
import com.plateforme.scolaire.repository.EnseignantRepository;
import com.plateforme.scolaire.repository.InscriptionRepository;
import com.plateforme.scolaire.repository.RendezVousRepository;
import com.plateforme.scolaire.repository.UtilisateurRepository;
import com.plateforme.scolaire.security.PerimetreService;
import com.plateforme.scolaire.web.dto.EnseignantDeLEleveDto;
import com.plateforme.scolaire.web.dto.RendezVousDto;
import com.plateforme.scolaire.web.dto.RendezVousRequest;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Rendez-vous entre une famille et un enseignant de la classe de l'élève. */
@Service
@RequiredArgsConstructor
@Transactional
public class RendezVousService {

    private final RendezVousRepository rendezVous;
    private final EleveRepository eleves;
    private final EnseignantRepository enseignants;
    private final InscriptionRepository inscriptions;
    private final AffectationEnseignantRepository affectations;
    private final UtilisateurRepository utilisateurs;
    private final PerimetreService perimetre;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<EnseignantDeLEleveDto> enseignantsDe(Long eleveId) {
        Long classeId = classeActive(eleveId);
        if (classeId == null) {
            return List.of();
        }
        Map<Long, EnseignantDeLEleveDto> parEnseignant = new LinkedHashMap<>();
        for (AffectationEnseignant a : affectations.findByClasseId(classeId)) {
            var u = a.getEnseignant().getUtilisateur();
            String nom = ((u.getPrenom() == null ? "" : u.getPrenom()) + " " + (u.getNom() == null ? "" : u.getNom())).trim();
            parEnseignant.computeIfAbsent(a.getEnseignant().getId(),
                    id -> new EnseignantDeLEleveDto(id, nom, new java.util.ArrayList<>())).matieres()
                    .add(a.getMatiere().getLibelle());
        }
        return List.copyOf(parEnseignant.values());
    }

    @Transactional(readOnly = true)
    public List<RendezVousDto> deLEleve(Long eleveId) {
        return rendezVous.deLEleve(eleveId).stream().map(RendezVousDto::de).toList();
    }

    @Transactional(readOnly = true)
    public List<RendezVousDto> demandesDeLEnseignant() {
        return rendezVous.aVenirDeLEnseignant(perimetre.utilisateurCourant().getId()).stream().map(RendezVousDto::de).toList();
    }

    public RendezVousDto demander(Long eleveId, RendezVousRequest r) {
        Long classeId = classeActive(eleveId);
        if (classeId == null || !affectations.findByClasseId(classeId).stream()
                .anyMatch(a -> a.getEnseignant().getId().equals(r.enseignantId()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cet enseignant n'enseigne pas dans la classe de l'élève");
        }
        if (!r.dateHeure().isAfter(LocalDateTime.now(clock))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le rendez-vous doit être dans le futur");
        }
        RendezVous rdv = new RendezVous();
        rdv.setEleve(eleves.getReferenceById(eleveId));
        rdv.setEnseignant(enseignants.getReferenceById(r.enseignantId()));
        rdv.setDemandePar(utilisateurs.getReferenceById(perimetre.utilisateurCourant().getId()));
        rdv.setDateHeure(r.dateHeure());
        rdv.setMotif(r.motif().trim());
        rdv.setCreatedAt(Instant.now(clock));
        Long id = rendezVous.save(rdv).getId();
        return rendezVous.deLEleve(eleveId).stream().filter(x -> x.getId().equals(id)).findFirst()
                .map(RendezVousDto::de).orElseThrow();
    }

    /** L'enseignant concerné confirme ou refuse. */
    public RendezVousDto repondre(Long id, boolean accepter, String reponse) {
        RendezVous r = charger(id);
        if (!r.getEnseignant().getUtilisateur().getId().equals(perimetre.utilisateurCourant().getId())) {
            throw new AccessDeniedException("Ce rendez-vous ne vous est pas destiné");
        }
        if (!"DEMANDE".equals(r.getStatut())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce rendez-vous a déjà reçu une réponse");
        }
        r.setStatut(accepter ? "CONFIRME" : "REFUSE");
        r.setReponse(reponse == null || reponse.isBlank() ? null : reponse.trim());
        return RendezVousDto.de(r);
    }

    /** La famille annule sa demande ou un rendez-vous confirmé. */
    public RendezVousDto annuler(Long id) {
        RendezVous r = charger(id);
        if (!perimetre.peutAccederEleve(r.getEleve().getId())) {
            throw new AccessDeniedException("Cet élève n'est pas rattaché à votre compte");
        }
        if ("ANNULE".equals(r.getStatut()) || "REFUSE".equals(r.getStatut())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce rendez-vous n'est plus actif");
        }
        r.setStatut("ANNULE");
        return RendezVousDto.de(r);
    }

    private RendezVous charger(Long id) {
        return rendezVous.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rendez-vous introuvable"));
    }

    private Long classeActive(Long eleveId) {
        return inscriptions.findByEleveIdAndStatutOrderByAnneeScolaireDateDebutDesc(eleveId, "ACCEPTEE").stream()
                .filter(i -> i.getClasse() != null).map(i -> i.getClasse().getId()).findFirst().orElse(null);
    }
}
