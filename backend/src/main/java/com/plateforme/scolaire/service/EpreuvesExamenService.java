package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.EpreuveExamen;
import com.plateforme.scolaire.domain.ExamenNational;
import com.plateforme.scolaire.repository.EpreuveExamenRepository;
import com.plateforme.scolaire.repository.ExamenNationalRepository;
import com.plateforme.scolaire.repository.MatiereRepository;
import com.plateforme.scolaire.security.PerimetreService;
import com.plateforme.scolaire.web.dto.EpreuveDto;
import com.plateforme.scolaire.web.dto.EpreuveRequest;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Emploi du temps officiel d'un examen : les épreuves (matière, date, créneau), publiées par l'organisateur. */
@Service
@RequiredArgsConstructor
@Transactional
public class EpreuvesExamenService {

    private final EpreuveExamenRepository epreuves;
    private final ExamenNationalRepository examens;
    private final MatiereRepository matieres;
    private final PerimetreService perimetre;
    private final AuditService audit;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<EpreuveDto> lister(Long examenId) {
        examen(examenId);
        return epreuves.findByExamenIdOrderByDateEpreuveAscHeureDebutAsc(examenId).stream().map(EpreuveDto::de).toList();
    }

    public EpreuveDto ajouter(Long examenId, EpreuveRequest r) {
        ExamenNational examen = examen(examenId);
        verifierGestion(examen);
        verifier(examen, r);
        EpreuveExamen e = new EpreuveExamen();
        e.setExamen(examen);
        if (r.matiereId() != null) {
            e.setMatiere(matieres.findById(r.matiereId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Matière inconnue")));
        } else {
            e.setLibelle(r.libelle().trim());
        }
        e.setDateEpreuve(r.date());
        e.setHeureDebut(r.heureDebut());
        e.setHeureFin(r.heureFin());
        e.setCoefficient(r.coefficient() == null ? 1 : r.coefficient());
        e.setCreatedAt(Instant.now(clock));
        EpreuveExamen enregistre = epreuves.save(e);
        audit.enregistrer("EPREUVE_EXAMEN_AJOUTEE", "ExamenNational", examenId,
                Map.of("epreuve", enregistre.nom(), "date", r.date().toString()));
        return EpreuveDto.de(enregistre);
    }

    public void supprimer(Long epreuveId) {
        EpreuveExamen e = epreuves.findById(epreuveId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Épreuve introuvable"));
        verifierGestion(e.getExamen());
        epreuves.delete(e);
        audit.enregistrer("EPREUVE_EXAMEN_SUPPRIMEE", "ExamenNational", e.getExamen().getId(), Map.of("epreuve", e.nom()));
    }

    /** Règles de saisie : une matière ou un libellé, un créneau cohérent, une date dans la période publiée. */
    static void verifier(ExamenNational examen, EpreuveRequest r) {
        if (r.matiereId() == null && (r.libelle() == null || r.libelle().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indiquez la matière de l'épreuve");
        }
        if (!r.heureFin().isAfter(r.heureDebut())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'heure de fin doit suivre l'heure de début");
        }
        if (examen.getDateDebut() != null && r.date().isBefore(examen.getDateDebut())
                || examen.getDateFin() != null && r.date().isAfter(examen.getDateFin())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La date est en dehors de la période de l'examen");
        }
    }

    private void verifierGestion(ExamenNational examen) {
        var principal = perimetre.utilisateurCourant();
        if (principal != null && !ExamenService.peutGererCalendrier(examen, principal.getAffectations())) {
            throw new AccessDeniedException("Le calendrier de cet examen est géré par son organisateur");
        }
    }

    private ExamenNational examen(Long id) {
        return examens.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Examen introuvable"));
    }
}
