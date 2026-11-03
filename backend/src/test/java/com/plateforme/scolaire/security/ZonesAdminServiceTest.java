package com.plateforme.scolaire.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.plateforme.scolaire.domain.Perimetre;
import com.plateforme.scolaire.domain.Role;
import com.plateforme.scolaire.domain.ZoneGeographique;
import java.util.List;
import org.junit.jupiter.api.Test;

class ZonesAdminServiceTest {

    private static ZoneGeographique zone(Long id, String type, ZoneGeographique parent) {
        ZoneGeographique z = new ZoneGeographique();
        z.setId(id);
        z.setTypeZone(type);
        z.setParent(parent);
        return z;
    }

    @Test
    void uneIreAdministreLesDirectionsLocalesDeSaRegionSeulement() {
        ZoneGeographique kindia = zone(1L, "REGION", null);
        ZoneGeographique coyah = zone(2L, "PREFECTURE", kindia);
        ZoneGeographique commune = zone(3L, "COMMUNE", coyah);
        ZoneGeographique conakry = zone(9L, "REGION", null);
        ZoneGeographique matoto = zone(10L, "COMMUNE", conakry);
        var ireKindia = List.of(new UtilisateurPrincipal.AffectationInfo(Role.DIRECTION_REGIONALE, Perimetre.ZONE, 1L, null));

        assertThat(ZonesAdminService.peutAdministrer(coyah, ireKindia)).isTrue();
        assertThat(ZonesAdminService.peutAdministrer(commune, ireKindia)).isTrue();
        assertThat(ZonesAdminService.peutAdministrer(matoto, ireKindia)).isFalse();
        // une région n'a pas de DPE/DCE : on ne crée pas ce compte sur une région
        assertThat(ZonesAdminService.peutAdministrer(kindia, ireKindia)).isFalse();
    }

    @Test
    void seuleUneIreDonneCeDroit() {
        ZoneGeographique kindia = zone(1L, "REGION", null);
        ZoneGeographique coyah = zone(2L, "PREFECTURE", kindia);
        var locale = List.of(new UtilisateurPrincipal.AffectationInfo(Role.DIRECTION_LOCALE, Perimetre.ZONE, 1L, null));
        var dgecs = List.of(new UtilisateurPrincipal.AffectationInfo(Role.DGECS, Perimetre.NATIONAL, null, null));

        assertThat(ZonesAdminService.peutAdministrer(coyah, locale)).isFalse();
        assertThat(ZonesAdminService.peutAdministrer(coyah, dgecs)).isFalse();
    }
}
