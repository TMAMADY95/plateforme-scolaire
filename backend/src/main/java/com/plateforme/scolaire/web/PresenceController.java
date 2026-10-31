package com.plateforme.scolaire.web;

import com.plateforme.scolaire.service.PresenceService;
import com.plateforme.scolaire.web.dto.AppelRequest;
import com.plateforme.scolaire.web.dto.FeuilleAppelDto;
import com.plateforme.scolaire.web.dto.PresenceDto;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PresenceController {

    private final PresenceService presenceService;
    private final com.plateforme.scolaire.service.AbsencesParentsService absencesParents;

    /** Feuille d'appel d'une classe (aujourd'hui si la date est omise), format de date : 2026-10-26. */
    @GetMapping("/api/classes/{classeId}/presences")
    @PreAuthorize("@perimetre.peutConsulterPresences(#classeId)")
    public FeuilleAppelDto feuille(@PathVariable Long classeId,
                                   @RequestParam(required = false)
                                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return presenceService.feuille(classeId, date);
    }

    @PutMapping("/api/classes/{classeId}/presences/{date}")
    @PreAuthorize("@perimetre.peutSaisirPresences(#classeId)")
    public FeuilleAppelDto enregistrer(@PathVariable Long classeId,
                                       @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                       @Valid @RequestBody AppelRequest requete) {
        FeuilleAppelDto feuille = presenceService.enregistrer(classeId, date, requete);
        absencesParents.prevenir(classeId, date);
        return feuille;
    }

    /** Historique d'un élève : visible par son établissement, ses parents et l'élève lui-même. */
    @GetMapping("/api/eleves/{id}/presences")
    @PreAuthorize("@perimetre.peutAccederEleve(#id)")
    public List<PresenceDto> historique(@PathVariable Long id,
                                        @RequestParam(required = false)
                                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
                                        @RequestParam(required = false)
                                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au) {
        return presenceService.historiqueEleve(id, du, au);
    }
}
