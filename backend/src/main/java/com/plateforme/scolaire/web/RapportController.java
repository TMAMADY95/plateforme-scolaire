package com.plateforme.scolaire.web;

import com.plateforme.scolaire.service.EvolutionService;
import com.plateforme.scolaire.service.RapportPdfService;
import com.plateforme.scolaire.web.dto.EvolutionDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Rapports officiels en PDF et évolution d'une année à l'autre. */
@RestController
@RequiredArgsConstructor
public class RapportController {

    private final RapportPdfService rapports;
    private final EvolutionService evolution;

    @GetMapping("/api/ministere/rapport.pdf")
    @PreAuthorize("hasRole('MINISTERE')")
    public ResponseEntity<byte[]> nationale(@RequestParam Long anneeScolaireId, @RequestParam(required = false) Long zoneId) {
        return pdf(rapports.nationale(anneeScolaireId, zoneId), "rapport-national.pdf");
    }

    @GetMapping("/api/region/rapport.pdf")
    @PreAuthorize("hasRole('DIRECTION_REGIONALE')")
    public ResponseEntity<byte[]> regionale(@RequestParam Long anneeScolaireId) {
        return pdf(rapports.regionale(anneeScolaireId), "rapport-regional.pdf");
    }

    @GetMapping("/api/etablissements/{id}/rapport.pdf")
    @PreAuthorize("@perimetre.peutGererEtablissement(#id) or (hasAnyRole('MINISTERE','DIRECTION_REGIONALE') "
            + "and @perimetre.peutAccederEtablissement(#id))")
    public ResponseEntity<byte[]> etablissement(@PathVariable Long id, @RequestParam Long anneeScolaireId) {
        return pdf(rapports.etablissement(id, anneeScolaireId), "rapport-etablissement-" + id + ".pdf");
    }

    @GetMapping("/api/ministere/evolution")
    @PreAuthorize("hasRole('MINISTERE')")
    public EvolutionDto evolutionNationale(@RequestParam(required = false) Long zoneId) {
        return evolution.nationale(zoneId);
    }

    @GetMapping("/api/region/evolution")
    @PreAuthorize("hasRole('DIRECTION_REGIONALE')")
    public EvolutionDto evolutionRegionale() {
        return evolution.regionale();
    }

    private static ResponseEntity<byte[]> pdf(byte[] contenu, String nom) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(nom).build().toString())
                .body(contenu);
    }
}
