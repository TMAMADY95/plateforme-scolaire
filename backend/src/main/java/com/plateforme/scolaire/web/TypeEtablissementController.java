package com.plateforme.scolaire.web;

import com.plateforme.scolaire.service.TypeEtablissementService;
import com.plateforme.scolaire.web.dto.TypeEtablissementDto;
import com.plateforme.scolaire.web.dto.TypeEtablissementRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/types-etablissement")
@RequiredArgsConstructor
public class TypeEtablissementController {

    private static final String GESTION = "hasAnyRole('MINISTERE', 'ADMIN_TECHNIQUE')";

    private final TypeEtablissementService service;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<TypeEtablissementDto> lister() {
        return service.lister();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(GESTION)
    public TypeEtablissementDto creer(@Valid @RequestBody TypeEtablissementRequest r) {
        return service.creer(r);
    }

    @PutMapping("/{id}")
    @PreAuthorize(GESTION)
    public TypeEtablissementDto modifier(@PathVariable Long id, @Valid @RequestBody TypeEtablissementRequest r) {
        return service.modifier(id, r);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(GESTION)
    public void supprimer(@PathVariable Long id) {
        service.supprimer(id);
    }
}
