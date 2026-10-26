package com.plateforme.scolaire.web;

import com.plateforme.scolaire.service.EtablissementService;
import com.plateforme.scolaire.web.dto.EtablissementDto;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/etablissements")
@RequiredArgsConstructor
public class EtablissementController {

    private final EtablissementService etablissementService;

    /** Liste filtrée selon le périmètre de l'utilisateur connecté. */
    @GetMapping
    public List<EtablissementDto> lister() {
        return etablissementService.listerAccessibles();
    }

    /** Modèle à suivre pour toute ressource rattachée à un établissement. */
    @GetMapping("/{id}")
    @PreAuthorize("@perimetre.peutAccederEtablissement(#id)")
    public EtablissementDto detail(@PathVariable Long id) {
        return etablissementService.trouver(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
