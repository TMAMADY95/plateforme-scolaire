package com.plateforme.scolaire.web;

import com.plateforme.scolaire.service.ListeCandidatsService;
import com.plateforme.scolaire.web.dto.ListeCandidatsDto;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Circuit de validation de la liste des candidats : établissement, direction régionale, ministère. */
@RestController
@RequiredArgsConstructor
public class ListeCandidatsController {

    private static final String SUPERVISION = "hasAnyRole('MINISTERE','DIRECTION_REGIONALE') and @perimetre.peutAccederEtablissement(#id)";

    private final ListeCandidatsService service;

    @GetMapping("/api/etablissements/{id}/examens/{examenId}/liste")
    @PreAuthorize("@perimetre.peutAccederEtablissement(#id)")
    public ListeCandidatsDto liste(@PathVariable Long id, @PathVariable Long examenId) {
        return service.liste(id, examenId);
    }

    @PostMapping("/api/etablissements/{id}/examens/{examenId}/liste/transmission")
    @PreAuthorize("@perimetre.peutGererEtablissement(#id)")
    public ListeCandidatsDto transmettre(@PathVariable Long id, @PathVariable Long examenId) {
        return service.transmettre(id, examenId);
    }

    @PostMapping("/api/etablissements/{id}/examens/{examenId}/liste/verification")
    @PreAuthorize(SUPERVISION)
    public ListeCandidatsDto verifier(@PathVariable Long id, @PathVariable Long examenId) {
        return service.validerRegion(id, examenId);
    }

    @PostMapping("/api/etablissements/{id}/examens/{examenId}/liste/validation")
    @PreAuthorize("hasRole('MINISTERE')")
    public ListeCandidatsDto valider(@PathVariable Long id, @PathVariable Long examenId) {
        return service.validerNationale(id, examenId);
    }

    @PostMapping("/api/etablissements/{id}/examens/{examenId}/liste/rejet")
    @PreAuthorize(SUPERVISION)
    public ListeCandidatsDto rejeter(@PathVariable Long id, @PathVariable Long examenId, @RequestBody Map<String, String> corps) {
        return service.rejeter(id, examenId, corps.get("motif"));
    }
}
