package com.plateforme.scolaire.web;

import com.plateforme.scolaire.service.AnnonceService;
import com.plateforme.scolaire.web.dto.AnnonceDto;
import com.plateforme.scolaire.web.dto.AnnonceRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AnnonceController {

    private final AnnonceService annonceService;

    /** Lecture : l'équipe de la classe, les élèves de la classe et leurs parents. */
    @GetMapping("/api/classes/{classeId}/annonces")
    @PreAuthorize("@perimetreEmploiDuTemps.peutConsulterClasse(#classeId)")
    public List<AnnonceDto> lister(@PathVariable Long classeId) {
        return annonceService.lister(classeId);
    }

    /** Publication : un enseignant de la classe, ou la direction / le personnel administratif de l'école. */
    @PostMapping("/api/classes/{classeId}/annonces")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@perimetreNotes.peutCreerEvaluation(#classeId) or @perimetre.peutGererClasse(#classeId)")
    public AnnonceDto publier(@PathVariable Long classeId, @Valid @RequestBody AnnonceRequest requete) {
        return annonceService.publier(classeId, requete);
    }

    @DeleteMapping("/api/annonces/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("isAuthenticated()")
    public void supprimer(@PathVariable Long id) {
        annonceService.supprimer(id);
    }
}
