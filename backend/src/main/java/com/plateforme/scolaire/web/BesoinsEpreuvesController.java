package com.plateforme.scolaire.web;

import com.plateforme.scolaire.security.DroitsExamen;
import com.plateforme.scolaire.service.BesoinsEpreuvesService;
import com.plateforme.scolaire.web.dto.BesoinsEpreuvesDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Besoins en personnel par épreuve : déclarés par la DGECS, consultés par le ministère. */
@RestController
@RequiredArgsConstructor
public class BesoinsEpreuvesController {

    private final BesoinsEpreuvesService service;

    @GetMapping("/api/examens/{examenId}/besoins-epreuves")
    @PreAuthorize("hasAnyRole('MINISTERE','DGECS')")
    public BesoinsEpreuvesDto suivi(@PathVariable Long examenId) {
        return service.suivi(examenId);
    }

    @PutMapping("/api/examens/{examenId}/epreuves/{epreuveId}/besoins")
    @PreAuthorize(DroitsExamen.NATIONAL)
    public BesoinsEpreuvesDto definir(@PathVariable Long examenId, @PathVariable Long epreuveId, @Valid @RequestBody BesoinsEpreuvesDto.Requete q) {
        return service.definir(examenId, epreuveId, q);
    }
}
