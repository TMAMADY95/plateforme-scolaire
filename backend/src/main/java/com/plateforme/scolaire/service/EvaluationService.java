package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.Classe;
import com.plateforme.scolaire.domain.Enseignant;
import com.plateforme.scolaire.domain.Evaluation;
import com.plateforme.scolaire.domain.Inscription;
import com.plateforme.scolaire.domain.Matiere;
import com.plateforme.scolaire.domain.Note;
import com.plateforme.scolaire.domain.Periode;
import com.plateforme.scolaire.domain.Utilisateur;
import com.plateforme.scolaire.repository.ClasseRepository;
import com.plateforme.scolaire.repository.EnseignantRepository;
import com.plateforme.scolaire.repository.EvaluationRepository;
import com.plateforme.scolaire.repository.InscriptionRepository;
import com.plateforme.scolaire.repository.MatiereRepository;
import com.plateforme.scolaire.repository.NoteRepository;
import com.plateforme.scolaire.repository.PeriodeRepository;
import com.plateforme.scolaire.repository.UtilisateurRepository;
import com.plateforme.scolaire.security.PerimetreNotesService;
import com.plateforme.scolaire.security.PerimetreService;
import com.plateforme.scolaire.security.UtilisateurPrincipal;
import com.plateforme.scolaire.web.dto.EleveDto;
import com.plateforme.scolaire.web.dto.EvaluationDto;
import com.plateforme.scolaire.web.dto.EvaluationModification;
import com.plateforme.scolaire.web.dto.EvaluationRequest;
import com.plateforme.scolaire.web.dto.GrilleNotesDto;
import com.plateforme.scolaire.web.dto.LigneNoteDto;
import com.plateforme.scolaire.web.dto.LigneNoteRequest;
import com.plateforme.scolaire.web.dto.NotesRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Évaluations et notes. Les autorisations de base (@PreAuthorize) sont dans les contrôleurs ;
 * ce service applique les règles métier. Toute modification ou suppression d'une note déjà saisie
 * est inscrite au journal d'audit, avec la valeur avant et après.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class EvaluationService {

    private final EvaluationRepository evaluationRepository;
    private final NoteRepository noteRepository;
    private final ClasseRepository classeRepository;
    private final MatiereRepository matiereRepository;
    private final PeriodeRepository periodeRepository;
    private final EnseignantRepository enseignantRepository;
    private final InscriptionRepository inscriptionRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final AuditService auditService;
    private final PerimetreService perimetreService;
    private final PerimetreNotesService perimetreNotesService;
    private final Clock clock;

    // ---------- Évaluations ----------

    @Transactional(readOnly = true)
    public List<EvaluationDto> lister(Long classeId, Long periodeId, Long matiereId) {
        if (!classeRepository.existsById(classeId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Classe introuvable");
        }
        return evaluationRepository.findByClasseIdOrderByDateEvaluationDescIdDesc(classeId).stream()
                .filter(e -> periodeId == null || e.getPeriode().getId().equals(periodeId))
                .filter(e -> matiereId == null || e.getMatiere().getId().equals(matiereId))
                .map(EvaluationDto::de)
                .toList();
    }

    public EvaluationDto creer(Long classeId, EvaluationRequest r) {
        Classe classe = classeRepository.findById(classeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Classe introuvable"));
        if (!perimetreNotesService.peutEvaluerMatiere(classeId, r.matiereId())) {
            throw new AccessDeniedException("Vous n'enseignez pas cette matière dans cette classe");
        }
        Matiere matiere = matiereRepository.findById(r.matiereId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Matière inconnue"));
        Periode periode = periodeRepository.findById(r.periodeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Période inconnue"));
        verifierPeriode(classe, periode, r.dateEvaluation());

        UtilisateurPrincipal courant = perimetreService.utilisateurCourant();
        Enseignant enseignant = courant == null ? null
                : enseignantRepository.findByUtilisateurId(courant.getId()).orElse(null);
        if (enseignant == null) {
            throw new AccessDeniedException("Seul un enseignant peut créer une évaluation");
        }

        Evaluation evaluation = new Evaluation();
        evaluation.setClasse(classe);
        evaluation.setMatiere(matiere);
        evaluation.setPeriode(periode);
        evaluation.setEnseignant(enseignant);
        evaluation.setTitre(r.titre().trim());
        evaluation.setDateEvaluation(r.dateEvaluation());
        evaluation.setCoefficient(r.coefficient() != null ? r.coefficient() : BigDecimal.ONE);
        evaluation.setBareme(r.bareme() != null ? r.bareme() : BigDecimal.valueOf(20));
        return EvaluationDto.de(evaluationRepository.save(evaluation));
    }

    public EvaluationDto modifier(Long id, EvaluationModification m) {
        Evaluation evaluation = charger(id);
        verifierPeriode(evaluation.getClasse(), evaluation.getPeriode(), m.dateEvaluation());
        BigDecimal bareme = m.bareme() != null ? m.bareme() : evaluation.getBareme();
        BigDecimal noteMax = noteRepository.maxValeur(id);
        if (noteMax != null && bareme.compareTo(noteMax) < 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Le barème ne peut pas être inférieur à une note déjà saisie (" + noteMax.toPlainString() + ")");
        }
        evaluation.setTitre(m.titre().trim());
        evaluation.setDateEvaluation(m.dateEvaluation());
        evaluation.setBareme(bareme);
        if (m.coefficient() != null) {
            evaluation.setCoefficient(m.coefficient());
        }
        return EvaluationDto.de(evaluation);
    }

    // ---------- Notes ----------

    @Transactional(readOnly = true)
    public GrilleNotesDto grille(Long evaluationId) {
        Evaluation evaluation = charger(evaluationId);
        return construire(evaluation, inscrits(evaluation), notesParInscription(evaluationId));
    }

    /** Enregistre, corrige ou efface des notes. Tout est validé avant la première écriture. */
    public GrilleNotesDto saisirNotes(Long evaluationId, NotesRequest requete) {
        Evaluation evaluation = charger(evaluationId);
        List<Inscription> inscrits = inscrits(evaluation);
        Map<Long, Inscription> inscritParId = new LinkedHashMap<>();
        inscrits.forEach(i -> inscritParId.put(i.getId(), i));

        Set<Long> vus = new HashSet<>();
        for (LigneNoteRequest ligne : requete.lignes()) {
            if (!inscritParId.containsKey(ligne.inscriptionId())) {
                throw invalide("L'inscription " + ligne.inscriptionId() + " n'est pas un élève accepté de cette classe");
            }
            if (!vus.add(ligne.inscriptionId())) {
                throw invalide("L'inscription " + ligne.inscriptionId() + " apparaît plusieurs fois");
            }
            if (ligne.estAbsent() && ligne.valeur() != null) {
                throw invalide("Un élève absent ne peut pas avoir de note");
            }
            if (ligne.valeur() != null && ligne.valeur().compareTo(evaluation.getBareme()) > 0) {
                throw invalide("La note " + ligne.valeur().toPlainString() + " dépasse le barème ("
                        + evaluation.getBareme().toPlainString() + ")");
            }
        }

        Map<Long, Note> notes = notesParInscription(evaluationId);
        UtilisateurPrincipal courant = perimetreService.utilisateurCourant();
        Utilisateur auteur = courant != null ? utilisateurRepository.getReferenceById(courant.getId()) : null;

        for (LigneNoteRequest ligne : requete.lignes()) {
            Long inscriptionId = ligne.inscriptionId();
            Note existante = notes.get(inscriptionId);
            String avant = representer(existante);

            if (!ligne.estAbsent() && ligne.valeur() == null) {
                if (existante != null) {
                    noteRepository.delete(existante);
                    notes.remove(inscriptionId);
                    auditer("SUPPRESSION_NOTE", existante, inscriptionId, avant, null);
                }
                continue;
            }

            Note note = existante != null ? existante : new Note();
            if (existante == null) {
                note.setEvaluation(evaluation);
                note.setInscription(inscritParId.get(inscriptionId));
            }
            note.setAbsent(ligne.estAbsent());
            note.setValeur(ligne.estAbsent() ? null : ligne.valeur().setScale(2, RoundingMode.HALF_UP));
            note.setModifiePar(auteur);
            note.setUpdatedAt(Instant.now(clock));
            notes.put(inscriptionId, noteRepository.save(note));

            String apres = representer(note);
            if (existante != null && !Objects.equals(avant, apres)) {
                auditer("MODIFICATION_NOTE", existante, inscriptionId, avant, apres);
            }
        }
        return construire(evaluation, inscrits, notes);
    }

    // ---------- Interne ----------

    private Evaluation charger(Long id) {
        return evaluationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Évaluation introuvable"));
    }

    private List<Inscription> inscrits(Evaluation evaluation) {
        return inscriptionRepository.findByClasseIdAndStatutOrderByEleveNomAscElevePrenomAsc(
                evaluation.getClasse().getId(), "ACCEPTEE");
    }

    private Map<Long, Note> notesParInscription(Long evaluationId) {
        Map<Long, Note> parInscription = new LinkedHashMap<>();
        noteRepository.findByEvaluationId(evaluationId)
                .forEach(n -> parInscription.put(n.getInscription().getId(), n));
        return parInscription;
    }

    private GrilleNotesDto construire(Evaluation evaluation, List<Inscription> inscrits, Map<Long, Note> notes) {
        List<LigneNoteDto> lignes = inscrits.stream().map(i -> {
            Note n = notes.get(i.getId());
            return new LigneNoteDto(i.getId(), EleveDto.de(i.getEleve()),
                    n != null ? n.getValeur() : null, n != null && n.isAbsent());
        }).toList();
        return new GrilleNotesDto(EvaluationDto.de(evaluation), lignes);
    }

    private void verifierPeriode(Classe classe, Periode periode, LocalDate date) {
        if (!periode.getAnneeScolaire().getId().equals(classe.getAnneeScolaire().getId())) {
            throw invalide("La période n'appartient pas à l'année scolaire de la classe");
        }
        if (periode.getEtablissement() != null && classe.getEtablissement() != null
                && !periode.getEtablissement().getId().equals(classe.getEtablissement().getId())) {
            throw invalide("Cette période appartient à un autre établissement");
        }
        if (date.isBefore(periode.getDateDebut()) || date.isAfter(periode.getDateFin())) {
            throw invalide("La date de l'évaluation est en dehors de la période");
        }
    }

    private void auditer(String action, Note noteExistante, Long inscriptionId, String avant, String apres) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("evaluationId", noteExistante.getEvaluation().getId());
        details.put("inscriptionId", inscriptionId);
        details.put("avant", avant);
        details.put("apres", apres);
        auditService.enregistrer(action, "Note", noteExistante.getId(), details);
    }

    private static String representer(Note note) {
        if (note == null) {
            return null;
        }
        if (note.isAbsent()) {
            return "ABSENT";
        }
        return note.getValeur() == null ? null : note.getValeur().setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static ResponseStatusException invalide(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
