package com.plateforme.scolaire.web;

import com.plateforme.scolaire.security.DroitsExamen;
import com.plateforme.scolaire.service.SujetsAImprimerService;
import com.plateforme.scolaire.service.TransportExamenService;
import com.plateforme.scolaire.web.dto.ConvoiExamenDto;
import com.plateforme.scolaire.web.dto.SujetsAImprimerDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Besoins d'impression et transport des sujets : gérés par la DGECS, consultés par le ministère. */
@RestController
@RequiredArgsConstructor
public class TransportSujetsController {

    private final TransportExamenService transport;
    private final SujetsAImprimerService sujets;

    @GetMapping("/api/examens/{examenId}/convois")
    @PreAuthorize("hasAnyRole('MINISTERE','DGECS')")
    public ConvoiExamenDto convois(@PathVariable Long examenId) {
        return transport.suivi(examenId);
    }

    @PostMapping("/api/examens/{examenId}/convois")
    @PreAuthorize(DroitsExamen.NATIONAL)
    @ResponseStatus(HttpStatus.CREATED)
    public ConvoiExamenDto creer(@PathVariable Long examenId, @Valid @RequestBody ConvoiExamenDto.Requete q) {
        return transport.creer(examenId, q);
    }

    @PutMapping("/api/examens/{examenId}/convois/{id}")
    @PreAuthorize(DroitsExamen.NATIONAL)
    public ConvoiExamenDto modifier(@PathVariable Long examenId, @PathVariable Long id, @Valid @RequestBody ConvoiExamenDto.Requete q) {
        return transport.modifier(examenId, id, q);
    }

    @DeleteMapping("/api/examens/{examenId}/convois/{id}")
    @PreAuthorize(DroitsExamen.NATIONAL)
    public ConvoiExamenDto supprimer(@PathVariable Long examenId, @PathVariable Long id) {
        return transport.supprimer(examenId, id);
    }

    @GetMapping("/api/examens/{examenId}/sujets-a-imprimer")
    @PreAuthorize("hasAnyRole('MINISTERE','DGECS')")
    public SujetsAImprimerDto sujets(@PathVariable Long examenId) {
        return sujets.besoins(examenId);
    }
}
