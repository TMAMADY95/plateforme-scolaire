package com.plateforme.scolaire.security;

import com.plateforme.scolaire.domain.CentreExamen;
import com.plateforme.scolaire.domain.Role;
import com.plateforme.scolaire.domain.ZoneGeographique;
import com.plateforme.scolaire.repository.AccesCentreRepository;
import com.plateforme.scolaire.repository.CentreExamenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Qui peut intervenir sur un centre d'examen, utilisable dans les annotations :
 * <pre>@PreAuthorize("... and @accesCentre.peut(#centreId)")</pre>
 * Ministère et DGECS : tous les centres. IRE et DPE/DCE : les centres de leur zone (et de ses sous-zones). Chef de centre :
 * seulement les centres qui lui sont confiés.
 */
@Service("accesCentre")
@RequiredArgsConstructor
public class AccesCentreService {

    private final CentreExamenRepository centres;
    private final AccesCentreRepository acces;
    private final PerimetreService perimetreService;

    @Transactional(readOnly = true)
    public boolean peut(Long centreId) {
        UtilisateurPrincipal p = perimetreService.utilisateurCourant();
        if (p == null || centreId == null) {
            return false;
        }
        CentreExamen centre = centres.findById(centreId).orElse(null);
        if (centre == null) {
            return true; // laisse le service répondre « centre introuvable »
        }
        for (UtilisateurPrincipal.AffectationInfo a : p.getAffectations()) {
            if (a.role() == Role.MINISTERE || a.role() == Role.DGECS) {
                return true;
            }
            if ((a.role() == Role.DIRECTION_REGIONALE || a.role() == Role.DIRECTION_LOCALE) && centreDansZone(centre, a.zoneId())) {
                return true;
            }
            if (a.role() == Role.CHEF_CENTRE && acces.existsByUtilisateurIdAndCentreId(p.getId(), centreId)) {
                return true;
            }
        }
        return false;
    }

    /** Le centre est dans la zone si sa zone, ou à défaut celle de son établissement, est sous cette zone. */
    static boolean centreDansZone(CentreExamen centre, Long zoneId) {
        if (zoneId == null) {
            return false;
        }
        ZoneGeographique zone = centre.getZone();
        if (zone == null && centre.getEtablissement() != null) {
            zone = centre.getEtablissement().getZone();
        }
        return zoneSousZone(zone, zoneId);
    }

    static boolean zoneSousZone(ZoneGeographique zone, Long zoneId) {
        for (ZoneGeographique z = zone; z != null; z = z.getParent()) {
            if (zoneId.equals(z.getId())) {
                return true;
            }
        }
        return false;
    }
}
