package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.Classe;
import com.plateforme.scolaire.domain.Devoir;
import com.plateforme.scolaire.domain.Enseignant;
import com.plateforme.scolaire.domain.Matiere;
import com.plateforme.scolaire.repository.ClasseRepository;
import com.plateforme.scolaire.repository.DevoirRepository;
import com.plateforme.scolaire.repository.EnseignantRepository;
import com.plateforme.scolaire.repository.MatiereRepository;
import com.plateforme.scolaire.security.PerimetreNotesService;
import com.plateforme.scolaire.security.PerimetreService;
import com.plateforme.scolaire.security.UtilisateurPrincipal;
import com.plateforme.scolaire.web.dto.DevoirDto;
import com.plateforme.scolaire.web.dto.DevoirRequest;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Devoirs publiés par les enseignants. Les autorisations de base sont dans le contrôleur. */
@Service
@RequiredArgsConstructor
@Transactional
public class DevoirService {

    /** Les devoirs échus depuis moins de ce nombre de jours restent visibles par défaut. */
    static final int JOURS_VISIBLES_APRES_ECHEANCE = 14;

    private final DevoirRepository devoirRepository;
    private final ClasseRepository classeRepository;
    private final MatiereRepository matiereRepository;
    private final EnseignantRepository enseignantRepository;
    private final PerimetreService perimetreService;
    private final PerimetreNotesService perimetreNotesService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<DevoirDto> lister(Long classeId, LocalDate depuis) {
        if (!classeRepository.existsById(classeId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Classe introuvable");
        }
        LocalDate debut = depuis != null ? depuis : LocalDate.now(clock).minusDays(JOURS_VISIBLES_APRES_ECHEANCE);
        return devoirRepository.findByClasseIdAndDateLimiteGreaterThanEqualOrderByDateLimiteAscIdAsc(classeId, debut)
                .stream().map(DevoirDto::de).toList();
    }

    public DevoirDto creer(Long classeId, DevoirRequest r) {
        Classe classe = classeRepository.findById(classeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Classe introuvable"));
        if (r.matiereId() == null) {
            throw invalide("La matière est obligatoire");
        }
        if (!perimetreNotesService.peutEvaluerMatiere(classeId, r.matiereId())) {
            throw new AccessDeniedException("Vous n'enseignez pas cette matière dans cette classe");
        }
        Matiere matiere = matiereRepository.findById(r.matiereId())
                .orElseThrow(() -> invalide("Matière inconnue"));
        verifierDate(r.dateLimite());

        UtilisateurPrincipal courant = perimetreService.utilisateurCourant();
        Enseignant enseignant = courant == null ? null
                : enseignantRepository.findByUtilisateurId(courant.getId()).orElse(null);
        if (enseignant == null) {
            throw new AccessDeniedException("Seul un enseignant peut publier un devoir");
        }

        Devoir devoir = new Devoir();
        devoir.setClasse(classe);
        devoir.setMatiere(matiere);
        devoir.setEnseignant(enseignant);
        devoir.setCreatedAt(Instant.now(clock));
        appliquer(devoir, r);
        return DevoirDto.de(devoirRepository.save(devoir));
    }

    public DevoirDto modifier(Long id, DevoirRequest r) {
        Devoir devoir = charger(id);
        verifierDate(r.dateLimite());
        appliquer(devoir, r);
        return DevoirDto.de(devoir);
    }

    public void supprimer(Long id) {
        devoirRepository.delete(charger(id));
    }

    private Devoir charger(Long id) {
        return devoirRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Devoir introuvable"));
    }

    private static void appliquer(Devoir devoir, DevoirRequest r) {
        devoir.setTitre(r.titre().trim());
        String consigne = r.consigne() == null ? "" : r.consigne().trim();
        devoir.setConsigne(consigne.isEmpty() ? null : consigne);
        devoir.setDateLimite(r.dateLimite());
    }

    private void verifierDate(LocalDate dateLimite) {
        if (dateLimite.isBefore(LocalDate.now(clock))) {
            throw invalide("La date limite ne peut pas être dans le passé");
        }
    }

    private static ResponseStatusException invalide(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
