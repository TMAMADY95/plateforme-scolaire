package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.CandidatExamen;
import com.plateforme.scolaire.domain.Eleve;
import com.plateforme.scolaire.domain.Etablissement;
import com.plateforme.scolaire.domain.ExamenNational;
import com.plateforme.scolaire.repository.CandidatExamenRepository;
import com.plateforme.scolaire.repository.EleveRepository;
import com.plateforme.scolaire.repository.EtablissementRepository;
import com.plateforme.scolaire.repository.ExamenNationalRepository;
import com.plateforme.scolaire.repository.ListeCandidatsRepository;
import com.plateforme.scolaire.web.dto.CandidatDto;
import com.plateforme.scolaire.web.dto.CandidatLibreRequest;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Candidature libre (règlement général) : au BEPC et au baccalauréat, un candidat non scolarisé dans une classe de l'année peut
 * déposer son dossier. L'établissement qui le reçoit l'inscrit comme candidat libre ; il n'est soumis ni à la règle des 25 %
 * d'absences ni à la participation aux évaluations, mais son dossier doit comporter la demande, l'acte de naissance et le
 * diplôme précédent. La candidature libre n'existe pas pour le CEE.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CandidatureLibreService {

    private final ExamenNationalRepository examens;
    private final EtablissementRepository etablissements;
    private final EleveRepository eleves;
    private final CandidatExamenRepository candidats;
    private final ListeCandidatsRepository listes;
    private final AuditService audit;
    private final Clock clock;

    /** La candidature libre n'est ouverte qu'au BEPC (examen régional) et au baccalauréat (examen national). */
    static boolean ouverte(String niveauOrganisation) {
        return DossierCandidatureService.diplomeRequis(niveauOrganisation);
    }

    public CandidatDto inscrire(Long etablissementId, Long examenId, CandidatLibreRequest r) {
        ExamenNational examen = examens.findById(examenId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Examen introuvable"));
        Etablissement etab = etablissements.findById(etablissementId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Établissement introuvable"));
        if (!ouverte(examen.getNiveauOrganisation())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La candidature libre n'existe qu'au BEPC et au baccalauréat");
        }
        if (!examen.accepteType(etab.getTypeEtablissement())
                || (examen.getCycle() != null && etab.getCycles().stream().noneMatch(c -> c.getId().equals(examen.getCycle().getId())))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cet examen n'est pas organisé dans votre établissement");
        }
        if (!"EN_COURS".equals(examen.getStatutResultats())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Les inscriptions sont closes pour cet examen");
        }
        if (listes.findByExamenIdAndEtablissementId(examenId, etablissementId).filter(l -> !"BROUILLON".equals(l.getStatut())).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La liste des candidats a été transmise : elle n'est plus modifiable");
        }
        String nom = r.nom().trim();
        String prenom = r.prenom().trim();
        List<Eleve> connus = eleves.findParIdentite(nom, prenom, r.dateNaissance());
        Eleve eleve = connus.size() == 1 ? connus.get(0) : null;
        if (connus.size() > 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Plusieurs dossiers portent cette identité : demandez à l'administration de les fusionner");
        }
        if (eleve != null && !candidats.elevesDejaInscrits(examenId, List.of(eleve.getId())).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, nom + " " + prenom + " est déjà candidat à cet examen");
        }
        if (eleve == null) {
            eleve = new Eleve();
            eleve.setMatricule(String.format("ELV-%08d", eleves.prochainNumeroMatricule()));
            eleve.setNom(nom);
            eleve.setPrenom(prenom);
            eleve.setDateNaissance(r.dateNaissance());
            eleve.setSexe(r.sexe());
            eleve = eleves.save(eleve);
        }
        CandidatExamen c = new CandidatExamen();
        c.setExamen(examen);
        c.setEleve(eleve);
        c.setEtablissement(etab);
        c.setLibre(true);
        c.setCreatedAt(Instant.now(clock));
        CandidatExamen enregistre = candidats.save(c);
        enregistre.setNumero(ExamenEtablissementService.numero(examen, enregistre.getId()));
        audit.enregistrer("CANDIDAT_LIBRE_INSCRIT", "ExamenNational", examenId, Map.of("etablissement", etab.getNom(), "eleve", eleve.getId()));
        return CandidatDto.de(enregistre);
    }
}
