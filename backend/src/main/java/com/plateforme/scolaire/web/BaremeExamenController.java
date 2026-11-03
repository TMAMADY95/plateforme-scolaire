package com.plateforme.scolaire.web;

import com.plateforme.scolaire.security.DroitsExamen;
import com.plateforme.scolaire.service.BaremeExamenService;
import com.plateforme.scolaire.web.dto.ExamenDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Barème des notes d'un examen : la DGECS organise les examens, elle seule le fixe. */
@RestController
@RequiredArgsConstructor
public class BaremeExamenController {

    public record BaremeRequest(@NotNull Integer bareme) {}

    private final BaremeExamenService service;

    @PutMapping("/api/examens/{examenId}/bareme")
    @PreAuthorize(DroitsExamen.NATIONAL)
    public ExamenDto definir(@PathVariable Long examenId, @Valid @RequestBody BaremeRequest r) {
        return service.definir(examenId, r.bareme());
    }
}
