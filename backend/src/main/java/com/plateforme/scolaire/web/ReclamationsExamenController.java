package com.plateforme.scolaire.web;

import com.plateforme.scolaire.security.DroitsExamen;
import com.plateforme.scolaire.service.QualiteDonneesExamensService;
import com.plateforme.scolaire.service.RectificationNoteService;
import com.plateforme.scolaire.service.ReclamationsExamenService;
import com.plateforme.scolaire.web.dto.QualiteDonneesDto;
import com.plateforme.scolaire.web.dto.ReclamationExamenDto;
import com.plateforme.scolaire.web.dto.ReclamationExamenRequest;
import com.plateforme.scolaire.web.dto.AnnulationRectificationRequest;
import com.plateforme.scolaire.web.dto.RectificationNoteRequest;
import java.security.Principal;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Réclamations sur les résultats et qualité des données des examens : gérées par la DGECS, consultées par le ministère. */
@RestController
@RequiredArgsConstructor
public class ReclamationsExamenController {

    private final ReclamationsExamenService service;
    private final QualiteDonneesExamensService qualite;
    private final RectificationNoteService rectification;

    @GetMapping("/api/examens/{examenId}/reclamations")
    @PreAuthorize("hasAnyRole('MINISTERE','DGECS')")
    public List<ReclamationExamenDto> liste(@PathVariable Long examenId) {
        return service.liste(examenId);
    }

    @PostMapping("/api/examens/{examenId}/reclamations")
    @PreAuthorize(DroitsExamen.NATIONAL)
    @ResponseStatus(HttpStatus.CREATED)
    public ReclamationExamenDto deposer(@PathVariable Long examenId, @Valid @RequestBody ReclamationExamenRequest q) {
        return service.deposer(examenId, q);
    }

    @PostMapping("/api/examens/{examenId}/reclamations/{id}/traitement")
    @PreAuthorize(DroitsExamen.NATIONAL)
    public ReclamationExamenDto traiter(@PathVariable Long examenId, @PathVariable Long id, @RequestBody Map<String, String> corps) {
        return service.traiter(examenId, id, corps.get("statut"), corps.get("decision"));
    }

    @PostMapping("/api/examens/{examenId}/reclamations/{id}/rectification")
    @PreAuthorize(DroitsExamen.NATIONAL)
    public ReclamationExamenDto rectifier(@PathVariable Long examenId, @PathVariable Long id, @Valid @RequestBody RectificationNoteRequest q, Principal principal) {
        return rectification.rectifier(examenId, id, q, principal == null ? null : principal.getName());
    }

    @PostMapping("/api/examens/{examenId}/reclamations/{id}/annulation-rectification")
    @PreAuthorize(DroitsExamen.NATIONAL)
    public ReclamationExamenDto annulerRectification(@PathVariable Long examenId, @PathVariable Long id, @Valid @RequestBody AnnulationRectificationRequest q,
            Principal principal) {
        return rectification.annuler(examenId, id, q.motif(), principal == null ? null : principal.getName());
    }

    @GetMapping("/api/dgecs/qualite-donnees")
    @PreAuthorize("hasAnyRole('MINISTERE','DGECS')")
    public QualiteDonneesDto qualite(@RequestParam Long anneeScolaireId) {
        return qualite.qualite(anneeScolaireId);
    }
}
