package com.plateforme.scolaire.web;

import com.plateforme.scolaire.service.CycleService;
import com.plateforme.scolaire.web.dto.CycleDto;
import com.plateforme.scolaire.web.dto.CycleRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cycles")
@RequiredArgsConstructor
public class CycleController {

    private static final String GESTION_REFERENTIEL = "hasAnyRole('MINISTERE', 'ADMIN_TECHNIQUE')";

    private final CycleService cycleService;

    @GetMapping
    public List<CycleDto> lister() {
        return cycleService.lister();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(GESTION_REFERENTIEL)
    public CycleDto creer(@Valid @RequestBody CycleRequest requete) {
        return cycleService.creer(requete);
    }

    @PutMapping("/{id}")
    @PreAuthorize(GESTION_REFERENTIEL)
    public CycleDto modifier(@PathVariable Long id, @Valid @RequestBody CycleRequest requete) {
        return cycleService.modifier(id, requete);
    }
}
