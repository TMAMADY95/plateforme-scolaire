package com.plateforme.scolaire.web;

import com.plateforme.scolaire.security.DroitsExamen;
import com.plateforme.scolaire.service.PersonnelNationalService;
import com.plateforme.scolaire.web.dto.PersonnelNationalDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Personnel mobilisé au niveau national pour un examen : géré par la DGECS, consulté par le ministère. */
@RestController
@RequiredArgsConstructor
public class PersonnelNationalController {

    private final PersonnelNationalService service;

    @GetMapping("/api/examens/{examenId}/personnel-national")
    @PreAuthorize("hasAnyRole('MINISTERE','DGECS')")
    public PersonnelNationalDto synthese(@PathVariable Long examenId) {
        return service.synthese(examenId);
    }

    @PostMapping("/api/examens/{examenId}/personnel-national")
    @PreAuthorize(DroitsExamen.NATIONAL)
    public PersonnelNationalDto ajouter(@PathVariable Long examenId, @Valid @RequestBody PersonnelNationalDto.AjoutRequest q) {
        return service.ajouter(examenId, q);
    }

    @DeleteMapping("/api/examens/{examenId}/personnel-national/{id}")
    @PreAuthorize(DroitsExamen.NATIONAL)
    public PersonnelNationalDto retirer(@PathVariable Long examenId, @PathVariable Long id) {
        return service.retirer(examenId, id);
    }

    @PutMapping("/api/examens/{examenId}/personnel-national/besoins")
    @PreAuthorize(DroitsExamen.NATIONAL)
    public PersonnelNationalDto besoin(@PathVariable Long examenId, @Valid @RequestBody PersonnelNationalDto.BesoinRequest q) {
        return service.definirBesoin(examenId, q);
    }
}
