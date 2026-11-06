package com.plateforme.scolaire.web;

import com.plateforme.scolaire.security.DroitsExamen;
import com.plateforme.scolaire.service.AnnuaireStatistiqueService;
import com.plateforme.scolaire.web.dto.AnnuaireDto;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AnnuaireStatistiqueController {

    private final AnnuaireStatistiqueService service;

    @GetMapping("/api/annuaire")
    @PreAuthorize(DroitsExamen.CONSULTATION)
    public AnnuaireDto annuaire(@RequestParam Long anneeId) {
        return service.annuaire(anneeId);
    }

    @GetMapping(value = "/api/annuaire.csv", produces = "text/csv;charset=UTF-8")
    @PreAuthorize(DroitsExamen.CONSULTATION)
    public ResponseEntity<byte[]> csv(@RequestParam Long anneeId) {
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"annuaire-statistique.csv\"")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(service.csv(anneeId).getBytes(StandardCharsets.UTF_8));
    }
}
