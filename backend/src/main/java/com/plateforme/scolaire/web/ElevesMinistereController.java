package com.plateforme.scolaire.web;

import com.plateforme.scolaire.service.ElevesMinistereService;
import com.plateforme.scolaire.service.ElevesMinistereService.Filtres;
import com.plateforme.scolaire.web.dto.ElevesPageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Liste nationale des élèves, réservée au ministère. */
@RestController
@RequiredArgsConstructor
public class ElevesMinistereController {

    private final ElevesMinistereService service;

    @GetMapping("/api/ministere/eleves")
    @PreAuthorize("hasRole('MINISTERE')")
    public ElevesPageDto lister(@RequestParam Long anneeScolaireId, @RequestParam(required = false) String q,
                                @RequestParam(required = false) Long zoneId, @RequestParam(required = false) Long etablissementId,
                                @RequestParam(required = false) String secteur,
                                @RequestParam(required = false) Long typeEtablissementId,
                                @RequestParam(required = false) Long cycleId, @RequestParam(required = false) Long niveauId,
                                @RequestParam(required = false) String sexe, @RequestParam(required = false) String statut,
                                @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "25") int taille) {
        return service.lister(new Filtres(anneeScolaireId, q, zoneId, etablissementId, secteur, typeEtablissementId,
                cycleId, niveauId, sexe, statut), page, taille);
    }

    @GetMapping("/api/ministere/eleves.pdf")
    @PreAuthorize("hasRole('MINISTERE')")
    public ResponseEntity<byte[]> pdf(@RequestParam Long anneeScolaireId, @RequestParam(required = false) String q,
                                      @RequestParam(required = false) Long zoneId,
                                      @RequestParam(required = false) Long etablissementId,
                                      @RequestParam(required = false) String secteur,
                                      @RequestParam(required = false) Long typeEtablissementId,
                                      @RequestParam(required = false) Long cycleId,
                                      @RequestParam(required = false) Long niveauId,
                                      @RequestParam(required = false) String sexe,
                                      @RequestParam(required = false) String statut) {
        byte[] contenu = service.exporterPdf(new Filtres(anneeScolaireId, q, zoneId, etablissementId, secteur,
                typeEtablissementId, cycleId, niveauId, sexe, statut));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"liste-eleves.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(contenu);
    }
}
