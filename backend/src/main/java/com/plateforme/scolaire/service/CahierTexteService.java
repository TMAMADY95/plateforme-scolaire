package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.CahierTexte;
import com.plateforme.scolaire.repository.CahierTexteRepository;
import com.plateforme.scolaire.repository.ClasseRepository;
import com.plateforme.scolaire.repository.EnseignantRepository;
import com.plateforme.scolaire.repository.InscriptionRepository;
import com.plateforme.scolaire.repository.MatiereRepository;
import com.plateforme.scolaire.security.PerimetreNotesService;
import com.plateforme.scolaire.security.PerimetreService;
import com.plateforme.scolaire.web.dto.CahierTexteDto;
import com.plateforme.scolaire.web.dto.CahierTexteRequest;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Cahier de texte : l'enseignant note ce qui a été fait en classe, les familles le consultent. */
@Service
@RequiredArgsConstructor
@Transactional
public class CahierTexteService {

    static final int JOURS_PAR_DEFAUT = 30;
    static final int JOURS_MAX = 366;

    private final CahierTexteRepository cahiers;
    private final ClasseRepository classes;
    private final MatiereRepository matieres;
    private final EnseignantRepository enseignants;
    private final InscriptionRepository inscriptions;
    private final PerimetreService perimetre;
    private final PerimetreNotesService perimetreNotes;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<CahierTexteDto> lister(Long classeId, LocalDate du, LocalDate au) {
        if (!classes.existsById(classeId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Classe introuvable");
        }
        LocalDate fin = au != null ? au : LocalDate.now(clock);
        LocalDate debut = du != null ? du : fin.minusDays(JOURS_PAR_DEFAUT);
        if (debut.isAfter(fin) || ChronoUnit.DAYS.between(debut, fin) > JOURS_MAX) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Période invalide (366 jours au plus)");
        }
        return cahiers.findByClasseIdAndDateSeanceBetweenOrderByDateSeanceDescIdDesc(classeId, debut, fin)
                .stream().map(CahierTexteDto::de).toList();
    }

    @Transactional(readOnly = true)
    public List<CahierTexteDto> deLEleve(Long eleveId) {
        Long classeId = inscriptions.findByEleveIdAndStatutOrderByAnneeScolaireDateDebutDesc(eleveId, "ACCEPTEE").stream()
                .filter(i -> i.getClasse() != null).map(i -> i.getClasse().getId()).findFirst().orElse(null);
        return classeId == null ? List.of() : lister(classeId, null, null);
    }

    public CahierTexteDto creer(Long classeId, CahierTexteRequest r) {
        var classe = classes.findById(classeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Classe introuvable"));
        if (!perimetreNotes.peutEvaluerMatiere(classeId, r.matiereId())) {
            throw new AccessDeniedException("Vous n'enseignez pas cette matière dans cette classe");
        }
        if (r.dateSeance().isAfter(LocalDate.now(clock))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La séance ne peut pas être dans le futur");
        }
        var matiere = matieres.findById(r.matiereId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Matière inconnue"));
        var courant = perimetre.utilisateurCourant();
        var enseignant = courant == null ? null : enseignants.findByUtilisateurId(courant.getId()).orElse(null);
        if (enseignant == null) {
            throw new AccessDeniedException("Seul un enseignant renseigne le cahier de texte");
        }
        CahierTexte c = new CahierTexte();
        c.setClasse(classe);
        c.setMatiere(matiere);
        c.setEnseignant(enseignant);
        c.setDateSeance(r.dateSeance());
        c.setContenu(r.contenu().trim());
        c.setCreatedAt(Instant.now(clock));
        return CahierTexteDto.de(cahiers.save(c));
    }

    public void supprimer(Long id) {
        CahierTexte c = cahiers.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entrée introuvable"));
        Long classeId = c.getClasse().getId();
        if (!perimetreNotes.peutEvaluerMatiere(classeId, c.getMatiere().getId()) && !perimetre.peutGererClasse(classeId)) {
            throw new AccessDeniedException("Entrée hors de votre périmètre");
        }
        cahiers.delete(c);
    }
}
