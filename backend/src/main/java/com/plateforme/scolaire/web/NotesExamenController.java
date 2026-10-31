package com.plateforme.scolaire.web;

import com.plateforme.scolaire.service.NotesExamenService;
import com.plateforme.scolaire.web.dto.NotesCandidatsDto;
import com.plateforme.scolaire.web.dto.SaisieNotesRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Notes individuelles des candidats d'un établissement : ministère et directions régionales de son périmètre. */
@RestController
@RequiredArgsConstructor
public class NotesExamenController {

    private static final String DROIT = "hasAnyRole('MINISTERE','DIRECTION_REGIONALE') and @perimetre.peutAccederEtablissement(#etablissementId)";

    private final NotesExamenService service;

    @GetMapping("/api/examens/{examenId}/etablissements/{etablissementId}/notes")
    @PreAuthorize(DROIT)
    public NotesCandidatsDto notes(@PathVariable Long examenId, @PathVariable Long etablissementId) {
        return service.notes(examenId, etablissementId);
    }

    @PutMapping("/api/examens/{examenId}/etablissements/{etablissementId}/candidats/{candidatId}/notes")
    @PreAuthorize(DROIT)
    public NotesCandidatsDto.Candidat saisir(@PathVariable Long examenId, @PathVariable Long etablissementId,
                                             @PathVariable Long candidatId, @RequestBody SaisieNotesRequest r) {
        return service.saisir(examenId, etablissementId, candidatId, r);
    }

    /** Déduit les résultats de l'établissement (présents, admis, absents, éliminés, filles) de ses candidats. */
    @PostMapping("/api/examens/{examenId}/etablissements/{etablissementId}/consolidation")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(DROIT)
    public void consolider(@PathVariable Long examenId, @PathVariable Long etablissementId) {
        service.consolider(examenId, etablissementId);
    }
}
