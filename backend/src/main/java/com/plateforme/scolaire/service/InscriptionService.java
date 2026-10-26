package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.AnneeScolaire;
import com.plateforme.scolaire.domain.Classe;
import com.plateforme.scolaire.domain.Eleve;
import com.plateforme.scolaire.domain.Etablissement;
import com.plateforme.scolaire.domain.Inscription;
import com.plateforme.scolaire.repository.AnneeScolaireRepository;
import com.plateforme.scolaire.repository.ClasseRepository;
import com.plateforme.scolaire.repository.EleveRepository;
import com.plateforme.scolaire.repository.EtablissementRepository;
import com.plateforme.scolaire.repository.InscriptionRepository;
import com.plateforme.scolaire.repository.UtilisateurRepository;
import com.plateforme.scolaire.security.PerimetreService;
import com.plateforme.scolaire.security.UtilisateurPrincipal;
import com.plateforme.scolaire.web.dto.DecisionInscription;
import com.plateforme.scolaire.web.dto.DecisionRequest;
import com.plateforme.scolaire.web.dto.EleveRequest;
import com.plateforme.scolaire.web.dto.InscriptionDto;
import com.plateforme.scolaire.web.dto.InscriptionRequest;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Cycle de vie d'une inscription : EN_ATTENTE, puis ACCEPTEE ou REFUSEE ; ANNULEE depuis EN_ATTENTE ou ACCEPTEE.
 * Les autorisations (@PreAuthorize) sont vérifiées dans les contrôleurs ; ce service applique les règles métier.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class InscriptionService {

    static final String EN_ATTENTE = "EN_ATTENTE";
    static final String ACCEPTEE = "ACCEPTEE";
    static final String REFUSEE = "REFUSEE";
    static final String ANNULEE = "ANNULEE";

    private final InscriptionRepository inscriptionRepository;
    private final EleveRepository eleveRepository;
    private final EtablissementRepository etablissementRepository;
    private final AnneeScolaireRepository anneeRepository;
    private final ClasseRepository classeRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final PerimetreService perimetreService;

    @Transactional(readOnly = true)
    public List<InscriptionDto> lister(Long etablissementId, Long anneeScolaireId, String statut) {
        if (!etablissementRepository.existsById(etablissementId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Établissement introuvable");
        }
        List<Inscription> inscriptions = anneeScolaireId == null
                ? inscriptionRepository.findByEtablissementIdOrderByDateDemandeDescIdDesc(etablissementId)
                : inscriptionRepository.findByEtablissementIdAndAnneeScolaireIdOrderByDateDemandeDescIdDesc(
                        etablissementId, anneeScolaireId);
        return inscriptions.stream()
                .filter(i -> statut == null || statut.isBlank() || i.getStatut().equals(statut))
                .map(InscriptionDto::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public InscriptionDto trouver(Long id) {
        return InscriptionDto.de(charger(id));
    }

    public InscriptionDto demander(Long etablissementId, InscriptionRequest r) {
        Etablissement etablissement = etablissementRepository.findById(etablissementId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Établissement introuvable"));
        if (!"ACTIF".equals(etablissement.getStatut())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "L'établissement n'est pas actif");
        }
        AnneeScolaire annee = anneeRepository.findById(r.anneeScolaireId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Année scolaire inconnue"));

        boolean avecMatricule = r.matriculeEleve() != null && !r.matriculeEleve().isBlank();
        boolean avecNouvelEleve = r.nouvelEleve() != null;
        if (avecMatricule == avecNouvelEleve) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Indiquer soit le matricule d'un élève existant, soit les données d'un nouvel élève");
        }

        Eleve eleve = avecMatricule
                ? eleveRepository.findByMatricule(r.matriculeEleve().trim())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "Aucun élève avec ce matricule"))
                : creerEleve(r.nouvelEleve(), Boolean.TRUE.equals(r.confirmerHomonyme()));

        if (inscriptionRepository.findByEleveIdAndAnneeScolaireIdAndStatutIn(
                eleve.getId(), annee.getId(), List.of(EN_ATTENTE, ACCEPTEE)).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cet élève a déjà une inscription active pour cette année");
        }

        Inscription inscription = new Inscription();
        inscription.setEleve(eleve);
        inscription.setEtablissement(etablissement);
        inscription.setAnneeScolaire(annee);
        inscription.setStatut(EN_ATTENTE);
        return InscriptionDto.de(inscriptionRepository.save(inscription));
    }

    public InscriptionDto decider(Long id, DecisionRequest r) {
        Inscription inscription = charger(id);
        if (!EN_ATTENTE.equals(inscription.getStatut())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette inscription a déjà été traitée");
        }
        if (r.decision() == DecisionInscription.ACCEPTEE) {
            affecterALaClasse(inscription, r.classeId());
            inscription.setStatut(ACCEPTEE);
        } else {
            inscription.setStatut(REFUSEE);
        }
        enregistrerDecision(inscription);
        return InscriptionDto.de(inscription);
    }

    public InscriptionDto annuler(Long id) {
        Inscription inscription = charger(id);
        if (!EN_ATTENTE.equals(inscription.getStatut()) && !ACCEPTEE.equals(inscription.getStatut())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Seule une inscription en attente ou acceptée peut être annulée");
        }
        inscription.setStatut(ANNULEE);
        enregistrerDecision(inscription);
        return InscriptionDto.de(inscription);
    }

    // ---------- Interne ----------

    private void affecterALaClasse(Inscription inscription, Long classeId) {
        if (classeId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Une classe est requise pour accepter");
        }
        Classe classe = classeRepository.findByIdPourMiseAJour(classeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Classe inconnue"));
        if (!classe.getEtablissement().getId().equals(inscription.getEtablissement().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La classe n'appartient pas à l'établissement de l'inscription");
        }
        if (!classe.getAnneeScolaire().getId().equals(inscription.getAnneeScolaire().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La classe n'appartient pas à l'année scolaire de l'inscription");
        }
        if (classe.getCapacite() != null
                && inscriptionRepository.countByClasseIdAndStatut(classe.getId(), ACCEPTEE) >= classe.getCapacite()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La classe est complète");
        }
        inscription.setClasse(classe);
    }

    private void enregistrerDecision(Inscription inscription) {
        inscription.setDateDecision(LocalDate.now());
        UtilisateurPrincipal courant = perimetreService.utilisateurCourant();
        if (courant != null) {
            inscription.setDecidePar(utilisateurRepository.getReferenceById(courant.getId()));
        }
    }

    private Eleve creerEleve(EleveRequest r, boolean confirmerHomonyme) {
        String nom = r.nom().trim();
        String prenom = r.prenom().trim();
        if (!confirmerHomonyme && !eleveRepository.findParIdentite(nom, prenom, r.dateNaissance()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Un élève de même identité existe déjà. S'il s'agit du même élève, utiliser son matricule ; "
                            + "sinon confirmer qu'il s'agit d'un homonyme (confirmerHomonyme = true).");
        }
        Eleve eleve = new Eleve();
        eleve.setMatricule(String.format("ELV-%08d", eleveRepository.prochainNumeroMatricule()));
        eleve.setNom(nom);
        eleve.setPrenom(prenom);
        eleve.setDateNaissance(r.dateNaissance());
        eleve.setSexe(r.sexe());
        return eleveRepository.save(eleve);
    }

    private Inscription charger(Long id) {
        return inscriptionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inscription introuvable"));
    }
}
