package com.plateforme.scolaire.security;

import com.plateforme.scolaire.domain.AffectationRole;
import com.plateforme.scolaire.domain.Role;
import com.plateforme.scolaire.domain.ZoneGeographique;
import com.plateforme.scolaire.repository.AffectationRoleRepository;
import com.plateforme.scolaire.repository.ZoneGeographiqueRepository;
import com.plateforme.scolaire.web.dto.CompteInstitutionnelDto;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hiérarchie administrative : les établissements relèvent des DPE/DCE, les DPE/DCE des IRE, les IRE du ministère.
 * Une IRE administre donc les comptes des directions préfectorales et communales situées dans sa région.
 * Utilisable dans les annotations : <pre>@zonesAdmin.peutAdministrerZone(#id)</pre>
 */
@Service("zonesAdmin")
@RequiredArgsConstructor
public class ZonesAdminService {

    private final ZoneGeographiqueRepository zones;
    private final AffectationRoleRepository affectations;
    private final PerimetreService perimetreService;

    /** Une IRE peut créer un compte DPE/DCE sur une préfecture ou une commune de sa région. */
    @Transactional(readOnly = true)
    public boolean peutAdministrerZone(Long zoneId) {
        UtilisateurPrincipal p = perimetreService.utilisateurCourant();
        if (p == null || zoneId == null) {
            return false;
        }
        ZoneGeographique zone = zones.findById(zoneId).orElse(null);
        return zone != null && peutAdministrer(zone, p.getAffectations());
    }

    /** Une IRE gère le compte d'une direction locale dont la zone est dans sa région (et seulement celle-là). */
    @Transactional(readOnly = true)
    public boolean peutGererCompteLocal(Long utilisateurId) {
        UtilisateurPrincipal p = perimetreService.utilisateurCourant();
        if (p == null || utilisateurId == null) {
            return false;
        }
        List<AffectationRole> cibles = affectations.findByUtilisateurId(utilisateurId);
        return !cibles.isEmpty() && cibles.stream().allMatch(a -> a.getRole() == Role.DIRECTION_LOCALE
                && a.getZone() != null && sousUneRegionDe(a.getZone(), p.getAffectations()));
    }

    /** Les directions préfectorales et communales de la région de l'IRE connectée. */
    @Transactional(readOnly = true)
    public List<CompteInstitutionnelDto> locauxDeMaRegion() {
        UtilisateurPrincipal p = perimetreService.utilisateurCourant();
        if (p == null) {
            return List.of();
        }
        return affectations.destinatairesTous(EnumSet.of(Role.DIRECTION_LOCALE)).stream()
                .filter(a -> a.getZone() != null && sousUneRegionDe(a.getZone(), p.getAffectations()))
                .sorted(Comparator.comparing((AffectationRole a) -> a.getZone().getNom(), String.CASE_INSENSITIVE_ORDER))
                .map(CompteInstitutionnelDto::de).toList();
    }

    /** Pur : la zone est une préfecture ou une commune placée sous la zone d'une des affectations IRE. */
    static boolean peutAdministrer(ZoneGeographique zone, Collection<UtilisateurPrincipal.AffectationInfo> affs) {
        boolean locale = "PREFECTURE".equalsIgnoreCase(zone.getTypeZone()) || "COMMUNE".equalsIgnoreCase(zone.getTypeZone());
        return locale && sousUneRegionDe(zone, affs);
    }

    static boolean sousUneRegionDe(ZoneGeographique zone, Collection<UtilisateurPrincipal.AffectationInfo> affs) {
        return affs.stream().anyMatch(a -> a.role() == Role.DIRECTION_REGIONALE && a.zoneId() != null
                && AccesCentreService.zoneSousZone(zone, a.zoneId()));
    }
}
