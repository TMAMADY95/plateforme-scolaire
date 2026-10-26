package com.plateforme.scolaire.web;

import com.plateforme.scolaire.service.EvaluationService;
import com.plateforme.scolaire.service.MoyennesService;
import com.plateforme.scolaire.web.dto.BulletinDto;
import com.plateforme.scolaire.web.dto.EvaluationDto;
import com.plateforme.scolaire.web.dto.EvaluationModification;
import com.plateforme.scolaire.web.dto.EvaluationRequest;
import com.plateforme.scolaire.web.dto.GrilleNotesDto;
import com.plateforme.scolaire.web.dto.NotesRequest;
import com.plateforme.scolaire.web.dto.ResultatsClasseDto;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class EvaluationController {

    private final EvaluationService evaluationService;
    private final MoyennesService moyennesService;

    @GetMapping("/api/classes/{classeId}/evaluations")
    @PreAuthorize("@perimetreNotes.peutConsulterClasse(#classeId)")
    public List<EvaluationDto> lister(@PathVariable Long classeId,
                                      @RequestParam(required = false) Long periodeId,
                                      @RequestParam(required = false) Long matiereId) {
        return evaluationService.lister(classeId, periodeId, matiereId);
    }

    @PostMapping("/api/classes/{classeId}/evaluations")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@perimetreNotes.peutCreerEvaluation(#classeId)")
    public EvaluationDto creer(@PathVariable Long classeId, @Valid @RequestBody EvaluationRequest requete) {
        return evaluationService.creer(classeId, requete);
    }

    /** Grille de notes : tous les élèves de la classe, avec leur note ou vide. */
    @GetMapping("/api/evaluations/{id}")
    @PreAuthorize("@perimetreNotes.peutConsulterEvaluation(#id)")
    public GrilleNotesDto grille(@PathVariable Long id) {
        return evaluationService.grille(id);
    }

    @PutMapping("/api/evaluations/{id}")
    @PreAuthorize("@perimetreNotes.peutModifierEvaluation(#id)")
    public EvaluationDto modifier(@PathVariable Long id, @Valid @RequestBody EvaluationModification requete) {
        return evaluationService.modifier(id, requete);
    }

    @PutMapping("/api/evaluations/{id}/notes")
    @PreAuthorize("@perimetreNotes.peutModifierEvaluation(#id)")
    public GrilleNotesDto saisirNotes(@PathVariable Long id, @Valid @RequestBody NotesRequest requete) {
        return evaluationService.saisirNotes(id, requete);
    }

    /** Moyennes et rangs de la classe pour une période. */
    @GetMapping("/api/classes/{classeId}/resultats")
    @PreAuthorize("@perimetreNotes.peutConsulterClasse(#classeId)")
    public ResultatsClasseDto resultats(@PathVariable Long classeId, @RequestParam Long periodeId) {
        return moyennesService.resultatsClasse(classeId, periodeId);
    }

    /** Bulletin : visible par l'établissement de l'élève, ses parents et l'élève lui-même. */
    @GetMapping("/api/eleves/{id}/bulletin")
    @PreAuthorize("@perimetre.peutAccederEleve(#id)")
    public BulletinDto bulletin(@PathVariable Long id, @RequestParam Long periodeId) {
        return moyennesService.bulletin(id, periodeId);
    }
}
