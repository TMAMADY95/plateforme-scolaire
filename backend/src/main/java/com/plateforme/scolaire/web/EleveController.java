package com.plateforme.scolaire.web;

import com.plateforme.scolaire.service.EleveService;
import com.plateforme.scolaire.web.dto.EleveDto;
import com.plateforme.scolaire.web.dto.EleveRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Les élèves sont créés via une demande d'inscription (voir InscriptionController). */
@RestController
@RequestMapping("/api/eleves")
@RequiredArgsConstructor
public class EleveController {

    private final EleveService eleveService;

    @GetMapping("/{id}")
    @PreAuthorize("@perimetre.peutAccederEleve(#id)")
    public EleveDto detail(@PathVariable Long id) {
        return eleveService.trouver(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perimetre.peutGererEleve(#id)")
    public EleveDto modifier(@PathVariable Long id, @Valid @RequestBody EleveRequest requete) {
        return eleveService.modifier(id, requete);
    }
}
