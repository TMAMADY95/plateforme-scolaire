package com.plateforme.scolaire.web;

import com.plateforme.scolaire.service.DevoirService;
import com.plateforme.scolaire.web.dto.DevoirDto;
import com.plateforme.scolaire.web.dto.DevoirRequest;
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

    /** Devoirs d'une classe, par date limite croissante. Par défaut : échus depuis moins de 14 jours ou à venir. */
    @GetMapping("/api/classes/{classeId}/devoirs")
    @PreAuthorize("@perimetreEmploiDuTemps.peutConsulterClasse(#classeId)")
    public List<DevoirDto> lister(@PathVariable Long classeId,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                  LocalDate depuis) {
        return devoirService.lister(classeId, depuis);
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
