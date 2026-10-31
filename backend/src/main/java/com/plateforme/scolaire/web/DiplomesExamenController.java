package com.plateforme.scolaire.web;

import com.plateforme.scolaire.service.DiplomesExamenService;
import com.plateforme.scolaire.web.dto.DiplomesExamenDto;
import com.plateforme.scolaire.web.dto.VerificationDiplomeDto;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Diplômes : délivrance et révocation par le ministère, PDF par établissement, vérification publique. */
@RestController
@RequiredArgsConstructor
public class DiplomesExamenController {

    private final DiplomesExamenService service;

    @GetMapping("/api/examens/{examenId}/diplomes")
    @PreAuthorize("hasRole('MINISTERE')")
    public DiplomesExamenDto diplomes(@PathVariable Long examenId) {
        return service.diplomes(examenId);
    }

    @PostMapping("/api/examens/{examenId}/diplomes")
    @PreAuthorize("hasRole('MINISTERE')")
    public DiplomesExamenDto delivrer(@PathVariable Long examenId) {
        return service.delivrer(examenId);
    }

    @PostMapping("/api/examens/{examenId}/diplomes/{diplomeId}/revocation")
    @PreAuthorize("hasRole('MINISTERE')")
    public DiplomesExamenDto revoquer(@PathVariable Long examenId, @PathVariable Long diplomeId, @RequestBody Map<String, String> corps) {
        return service.revoquer(examenId, diplomeId, corps.get("motif"));
    }

    @GetMapping("/api/etablissements/{id}/examens/{examenId}/diplomes.pdf")
    @PreAuthorize("@perimetre.peutAccederEtablissement(#id)")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id, @PathVariable Long examenId) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"diplomes.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(service.pdf(id, examenId));
    }

    /** Public : contrôle d'authenticité par le code imprimé sur le diplôme. */
    @GetMapping("/api/verification-diplome/{code}")
    public VerificationDiplomeDto verifier(@PathVariable String code) {
        return service.verifier(code);
    }
}
