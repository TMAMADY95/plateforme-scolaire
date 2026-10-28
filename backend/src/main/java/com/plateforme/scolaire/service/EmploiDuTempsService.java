package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.Classe;
import com.plateforme.scolaire.domain.Creneau;
import com.plateforme.scolaire.domain.Enseignant;
import com.plateforme.scolaire.domain.Etablissement;
import com.plateforme.scolaire.domain.Matiere;
import com.plateforme.scolaire.domain.Salle;
import com.plateforme.scolaire.repository.AffectationEnseignantRepository;
import com.plateforme.scolaire.repository.ClasseRepository;
import com.plateforme.scolaire.repository.CreneauRepository;
import com.plateforme.scolaire.repository.EnseignantRepository;
import com.plateforme.scolaire.repository.EtablissementRepository;
import com.plateforme.scolaire.repository.MatiereRepository;
import com.plateforme.scolaire.repository.SalleRepository;
import com.plateforme.scolaire.web.dto.CreneauDto;
import com.plateforme.scolaire.web.dto.CreneauEleveDto;
import com.plateforme.scolaire.web.dto.CreneauRequest;
import com.plateforme.scolaire.web.dto.SalleDto;
import com.plateforme.scolaire.web.dto.SalleRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Emplois du temps et salles (Module 9) : création des salles, saisie des créneaux par classe,
 * contrôle des conflits d'horaire (enseignant et salle) et consultation des emplois du temps.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class EmploiDuTempsService {

    private final SalleRepository salleRepository;
    private final CreneauRepository creneauRepository;
    private final ClasseRepository classeRepository;
    private final EtablissementRepository etablissementRepository;
    private final MatiereRepository matiereRepository;
    private final EnseignantRepository enseignantRepository;
    private final AffectationEnseignantRepository affectationRepository;

    // ---------- Salles ----------

    @Transactional(readOnly = true)
    public List<SalleDto> listerSalles(Long etablissementId) {
        if (!etablissementRepository.existsById(etablissementId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Établissement introuvable");
        }
        return salleRepository.findByEtablissementIdOrderByNom(etablissementId).stream()
                .map(SalleDto::de).toList();
    }

    public SalleDto creerSalle(Long etablissementId, SalleRequest r) {
        Etablissement etablissement = etablissementRepository.findById(etablissementId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Établissement introuvable"));
        String nom = r.nom().trim();
        if (salleRepository.existsByEtablissementIdAndNom(etablissementId, nom)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Une salle porte déjà ce nom dans cet établissement");
        }
        Salle salle = new Salle();
        salle.setEtablissement(etablissement);
        salle.setNom(nom);
        salle.setCapacite(r.capacite());
        salle.setTypeSalle(r.typeSalle());
        return SalleDto.de(salleRepository.save(salle));
    }

    public void supprimerSalle(Long etablissementId, Long salleId) {
        Salle salle = salleRepository.findById(salleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Salle introuvable"));
        if (!salle.getEtablissement().getId().equals(etablissementId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Salle introuvable dans cet établissement");
        }
        if (creneauRepository.existsBySalleId(salleId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cette salle est utilisée par des créneaux : modifiez-les ou supprimez-les d'abord");
        }
        salleRepository.delete(salle);
    }

    // ---------- Créneaux ----------

    @Transactional(readOnly = true)
    public List<CreneauDto> listerCreneauxClasse(Long classeId) {
        if (!classeRepository.existsById(classeId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Classe introuvable");
        }
        return creneauRepository.findByClasseIdOrderByJourSemaineAscHeureDebutAsc(classeId).stream()
                .map(CreneauDto::de).toList();
    }

    @Transactional(readOnly = true)
    public List<CreneauDto> listerCreneauxEtablissement(Long etablissementId, Long anneeScolaireId) {
        if (!etablissementRepository.existsById(etablissementId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Établissement introuvable");
        }
        return creneauRepository.findByEtablissementEtAnnee(etablissementId, anneeScolaireId).stream()
                .map(CreneauDto::de).toList();
    }

    public CreneauDto creerCreneau(Long classeId, CreneauRequest r) {
        Classe classe = chargerClasse(classeId);
        Matiere matiere = matiereRepository.findById(r.matiereId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Matière inconnue"));
        Enseignant enseignant = r.enseignantId() == null ? null
                : enseignantRepository.findById(r.enseignantId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enseignant inconnu"));
        Salle salle = r.salleId() == null ? null
                : chargerSalleDeLEtablissement(r.salleId(), classe.getEtablissement().getId());

        if (!r.heureFin().isAfter(r.heureDebut())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'heure de fin doit suivre l'heure de début");
        }
        verifierConflitClasse(classeId, r.jourSemaine(), r.heureDebut(), r.heureFin(), null);
        if (enseignant != null) {
            verifierEnseignantDeLEtablissement(enseignant, classe);
            verifierConflitEnseignant(enseignant.getId(), r.jourSemaine(), r.heureDebut(), r.heureFin(), null);
        }
        if (salle != null) {
            verifierConflitSalle(salle.getId(), r.jourSemaine(), r.heureDebut(), r.heureFin(), null);
        }

        Creneau creneau = new Creneau();
        creneau.setClasse(classe);
        creneau.setMatiere(matiere);
        creneau.setEnseignant(enseignant);
        creneau.setSalle(salle);
        creneau.setJourSemaine(r.jourSemaine());
        creneau.setHeureDebut(r.heureDebut());
        creneau.setHeureFin(r.heureFin());
        return CreneauDto.de(creneauRepository.save(creneau));
    }

    public CreneauDto modifierCreneau(Long creneauId, CreneauRequest r) {
        Creneau creneau = chargerCreneau(creneauId);
        Classe classe = creneau.getClasse();
        Matiere matiere = matiereRepository.findById(r.matiereId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Matière inconnue"));
        Enseignant enseignant = r.enseignantId() == null ? null
                : enseignantRepository.findById(r.enseignantId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enseignant inconnu"));
        Salle salle = r.salleId() == null ? null
                : chargerSalleDeLEtablissement(r.salleId(), classe.getEtablissement().getId());

        if (!r.heureFin().isAfter(r.heureDebut())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'heure de fin doit suivre l'heure de début");
        }
        verifierConflitClasse(classe.getId(), r.jourSemaine(), r.heureDebut(), r.heureFin(), creneauId);
        if (enseignant != null) {
            verifierEnseignantDeLEtablissement(enseignant, classe);
            verifierConflitEnseignant(enseignant.getId(), r.jourSemaine(), r.heureDebut(), r.heureFin(), creneauId);
        }
        if (salle != null) {
            verifierConflitSalle(salle.getId(), r.jourSemaine(), r.heureDebut(), r.heureFin(), creneauId);
        }

        creneau.setMatiere(matiere);
        creneau.setEnseignant(enseignant);
        creneau.setSalle(salle);
        creneau.setJourSemaine(r.jourSemaine());
        creneau.setHeureDebut(r.heureDebut());
        creneau.setHeureFin(r.heureFin());
        return CreneauDto.de(creneau);
    }

    public void supprimerCreneau(Long creneauId) {
        creneauRepository.delete(chargerCreneau(creneauId));
    }

    /** Emploi du temps d'un élève : les créneaux de la classe de son inscription active. */
    @Transactional(readOnly = true)
    public List<CreneauEleveDto> emploiDuTempsDeLaClasse(Long classeId) {
        if (!classeRepository.existsById(classeId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Classe introuvable");
        }
        return creneauRepository.findByClasseIdOrderByJourSemaineAscHeureDebutAsc(classeId).stream()
                .map(CreneauEleveDto::de).toList();
    }

    // ---------- Contrôles ----------

    /** Une classe ne peut pas avoir deux cours en même temps. */
    private void verifierConflitClasse(Long classeId, Short jour, java.time.LocalTime debut,
                                       java.time.LocalTime fin, Long exclureId) {
        List<Creneau> conflits = creneauRepository.chevauchementsClasse(classeId, jour, debut, fin, exclureId);
        if (!conflits.isEmpty()) {
            Creneau c = conflits.get(0);
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cette classe a déjà un cours sur ce créneau (" + c.getMatiere().getLibelle() + " "
                            + c.getHeureDebut() + "-" + c.getHeureFin() + ")");
        }
    }

    private void verifierConflitEnseignant(Long enseignantId, Short jour, java.time.LocalTime debut,
                                           java.time.LocalTime fin, Long exclureId) {
        List<Creneau> conflits = creneauRepository.chevauchementsEnseignant(enseignantId, jour, debut, fin, exclureId);
        if (!conflits.isEmpty()) {
            Creneau c = conflits.get(0);
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Conflit d'horaire pour cet enseignant avec le créneau " + c.getClasse().getNom()
                            + " (" + c.getMatiere().getLibelle() + " " + c.getHeureDebut() + "-" + c.getHeureFin() + ")");
        }
    }

    private void verifierConflitSalle(Long salleId, Short jour, java.time.LocalTime debut,
                                      java.time.LocalTime fin, Long exclureId) {
        List<Creneau> conflits = creneauRepository.chevauchementsSalle(salleId, jour, debut, fin, exclureId);
        if (!conflits.isEmpty()) {
            Creneau c = conflits.get(0);
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Conflit d'horaire pour cette salle avec le créneau " + c.getClasse().getNom()
                            + " (" + c.getMatiere().getLibelle() + " " + c.getHeureDebut() + "-" + c.getHeureFin() + ")");
        }
    }

    private void verifierEnseignantDeLEtablissement(Enseignant enseignant, Classe classe) {
        long rattachements = enseignantRepository.compterRattachements(
                enseignant.getId(), classe.getEtablissement().getId(), com.plateforme.scolaire.domain.Role.ENSEIGNANT);
        if (rattachements == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cet enseignant n'est pas rattaché à l'établissement de la classe");
        }
    }

    private Salle chargerSalleDeLEtablissement(Long salleId, Long etablissementId) {
        Salle salle = salleRepository.findById(salleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Salle inconnue"));
        if (!salle.getEtablissement().getId().equals(etablissementId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cette salle n'appartient pas à l'établissement de la classe");
        }
        return salle;
    }

    private Classe chargerClasse(Long id) {
        return classeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Classe introuvable"));
    }

    private Creneau chargerCreneau(Long id) {
        return creneauRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Créneau introuvable"));
    }
}
