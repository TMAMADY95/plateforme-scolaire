package com.plateforme.scolaire.web;

import com.plateforme.scolaire.service.DevoirFaitService;
import com.plateforme.scolaire.service.DevoirService;
import com.plateforme.scolaire.web.dto.DevoirDto;
import com.plateforme.scolaire.web.dto.DevoirRequest;
import com.plateforme.scolaire.web.dto.FaitRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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
public class DevoirController {

    private final DevoirService devoirService;
    private final DevoirFaitService devoirFaitService;

    /** Devoirs d'une classe, par date limite croissante. Par défaut : échus depuis moins de 14 jours ou à venir. */
    @GetMapping("/api/classes/{classeId}/devoirs")
    @PreAuthorize("@perimetreEmploiDuTemps.peutConsulterClasse(#classeId)")
    public List<DevoirDto> lister(@PathVariable Long classeId,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                  LocalDate depuis) {
        // L'équipe pédagogique voit en plus combien d'élèves ont terminé chaque devoir.
        return devoirFaitService.avecProgres(classeId, devoirService.lister(classeId, depuis));
    }

    /** Les devoirs de la classe de l'élève, avec son état « fait » (élève lui-même, ses parents, l'établissement). */
    @GetMapping("/api/eleves/{id}/devoirs")
    @PreAuthorize("@perimetre.peutAccederEleve(#id)")
    public List<DevoirDto> deLEleve(@PathVariable Long id) {
        return devoirFaitService.deLEleve(id);
    }

    /** Coche ou décoche un devoir : l'élève pour lui-même, un parent pour son enfant (contrôlé par le service). */
    @PutMapping("/api/devoirs/{id}/fait")
    @PreAuthorize("isAuthenticated()")
    public DevoirDto marquerFait(@PathVariable Long id, @Valid @RequestBody FaitRequest requete) {
        return devoirFaitService.marquer(id, requete.eleveId(), requete.fait());
    }

    @PostMapping("/api/classes/{classeId}/devoirs")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@perimetreNotes.peutCreerEvaluation(#classeId)")
    public DevoirDto creer(@PathVariable Long classeId, @Valid @RequestBody DevoirRequest requete) {
        return devoirService.creer(classeId, requete);
    }

    @PutMapping("/api/devoirs/{id}")
    @PreAuthorize("@perimetreDevoirs.peutModifier(#id)")
    public DevoirDto modifier(@PathVariable Long id, @Valid @RequestBody DevoirRequest requete) {
        return devoirService.modifier(id, requete);
    }

    @DeleteMapping("/api/devoirs/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@perimetreDevoirs.peutModifier(#id)")
    public void supprimer(@PathVariable Long id) {
        devoirService.supprimer(id);
    }
}
