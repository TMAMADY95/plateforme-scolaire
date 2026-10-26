package com.plateforme.scolaire.web;

import com.plateforme.scolaire.service.ParentService;
import com.plateforme.scolaire.web.dto.LienParentalDto;
import com.plateforme.scolaire.web.dto.ParentRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ParentController {

    private final ParentService parentService;

    @PostMapping("/api/eleves/{id}/parents")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@perimetre.peutGererEleve(#id)")
    public LienParentalDto lier(@PathVariable Long id, @Valid @RequestBody ParentRequest requete) {
        return parentService.lier(id, requete);
    }
}
