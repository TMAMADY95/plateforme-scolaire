package com.plateforme.scolaire.web;

import com.plateforme.scolaire.web.dto.ResultatExamenEleveDto;
import com.plateforme.scolaire.service.ResultatsExamensEleveService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** Résultats proclamés des examens nationaux d'un élève (élève, parents, personnel de son établissement). */
@RestController
@RequiredArgsConstructor
public class ResultatsExamensEleveController {

    private final ResultatsExamensEleveService service;

    @GetMapping("/api/eleves/{id}/resultats-examens")
    @PreAuthorize("@perimetre.peutAccederEleve(#id)")
    public List<ResultatExamenEleveDto> resultats(@PathVariable Long id) {
        return service.deLEleve(id);
    }
}
