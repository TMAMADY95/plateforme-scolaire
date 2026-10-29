package com.plateforme.scolaire.web;

import com.plateforme.scolaire.service.ReussiteReseauService;
import com.plateforme.scolaire.service.ReussiteService;
import com.plateforme.scolaire.service.StatistiquesService;
import com.plateforme.scolaire.web.dto.ReussiteDto;
import com.plateforme.scolaire.web.dto.ReussiteReseauDto;
import com.plateforme.scolaire.web.dto.StatistiquesDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Chiffres et graphiques des tableaux de bord, selon le périmètre de l'utilisateur. */
@RestController
@RequiredArgsConstructor
public class StatistiquesController {

    private final StatistiquesService statistiquesService;
    private final ReussiteService reussiteService;
    private final ReussiteReseauService reussiteReseauService;

    @GetMapping("/api/ministere/statistiques")
    @PreAuthorize("hasRole('MINISTERE')")
    public StatistiquesDto nationales(@RequestParam Long anneeScolaireId) {
        return statistiquesService.nationales(anneeScolaireId);
    }

    @GetMapping("/api/region/statistiques")
    @PreAuthorize("hasRole('DIRECTION_REGIONALE')")
    public StatistiquesDto regionales(@RequestParam Long anneeScolaireId) {
        return statistiquesService.regionales(anneeScolaireId);
    }

    @GetMapping("/api/etablissements/{etablissementId}/statistiques")
    @PreAuthorize("@perimetre.peutGererEtablissement(#etablissementId)")
    public StatistiquesDto etablissement(@PathVariable Long etablissementId, @RequestParam Long anneeScolaireId) {
        return statistiquesService.etablissement(etablissementId, anneeScolaireId);
    }

    /** Taux de réussite par classe pour une période (moyenne générale au moins égale à 10 sur 20). */
    @GetMapping("/api/etablissements/{etablissementId}/reussite")
    @PreAuthorize("@perimetre.peutGererEtablissement(#etablissementId)")
    public ReussiteDto reussite(@PathVariable Long etablissementId, @RequestParam Long anneeScolaireId,
                                @RequestParam Long periodeId) {
        return reussiteService.reussite(etablissementId, anneeScolaireId, periodeId);
    }

    @GetMapping("/api/ministere/reussite")
    @PreAuthorize("hasRole('MINISTERE')")
    public ReussiteReseauDto reussiteNationale(@RequestParam Long anneeScolaireId, @RequestParam Long periodeId) {
        return reussiteReseauService.nationale(anneeScolaireId, periodeId);
    }

    @GetMapping("/api/region/reussite")
    @PreAuthorize("hasRole('DIRECTION_REGIONALE')")
    public ReussiteReseauDto reussiteRegionale(@RequestParam Long anneeScolaireId, @RequestParam Long periodeId) {
        return reussiteReseauService.regionale(anneeScolaireId, periodeId);
    }
}
