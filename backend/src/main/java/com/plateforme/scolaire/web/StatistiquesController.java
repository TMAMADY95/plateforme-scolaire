package com.plateforme.scolaire.web;

import com.plateforme.scolaire.service.ReussiteReseauService;
import com.plateforme.scolaire.service.ReussiteService;
import com.plateforme.scolaire.service.StatistiquesService;
import com.plateforme.scolaire.web.dto.ReussiteDto;
import com.plateforme.scolaire.web.dto.ReussiteReseauDto;
import com.plateforme.scolaire.web.dto.StatistiquesDto;
import com.plateforme.scolaire.web.dto.LigneEtablissementStatDto;
import com.plateforme.scolaire.web.dto.LigneZoneStatDto;
import java.util.List;
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
    public StatistiquesDto nationales(@RequestParam Long anneeScolaireId, @RequestParam(required = false) Long zoneId) {
        return zoneId == null ? statistiquesService.nationales(anneeScolaireId)
                : statistiquesService.zone(zoneId, anneeScolaireId);
    }

    /** Une ligne par établissement, pour tout le pays ou pour une zone (région…) et ses sous-zones. */
    @GetMapping("/api/ministere/statistiques/etablissements")
    @PreAuthorize("hasRole('MINISTERE')")
    public List<LigneEtablissementStatDto> etablissementsNationaux(@RequestParam Long anneeScolaireId,
                                                                   @RequestParam(required = false) Long zoneId) {
        return statistiquesService.lignesNationales(anneeScolaireId, zoneId);
    }

    /** Une ligne par zone, sous-zones comprises. */
    @GetMapping("/api/ministere/statistiques/zones")
    @PreAuthorize("hasRole('MINISTERE')")
    public List<LigneZoneStatDto> zones(@RequestParam Long anneeScolaireId) {
        return statistiquesService.lignesParZone(anneeScolaireId);
    }

    @GetMapping("/api/region/statistiques/etablissements")
    @PreAuthorize("hasRole('DIRECTION_REGIONALE')")
    public List<LigneEtablissementStatDto> etablissementsRegionaux(@RequestParam Long anneeScolaireId) {
        return statistiquesService.lignesRegionales(anneeScolaireId);
    }

    @GetMapping("/api/region/statistiques")
    @PreAuthorize("hasRole('DIRECTION_REGIONALE')")
    public StatistiquesDto regionales(@RequestParam Long anneeScolaireId) {
        return statistiquesService.regionales(anneeScolaireId);
    }

    /** La direction de l'établissement, et le ministère ou la direction régionale pour les établissements de leur périmètre. */
    @GetMapping("/api/etablissements/{etablissementId}/statistiques")
    @PreAuthorize("@perimetre.peutGererEtablissement(#etablissementId) or "
            + "(hasAnyRole('MINISTERE', 'DIRECTION_REGIONALE') and @perimetre.peutAccederEtablissement(#etablissementId))")
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
