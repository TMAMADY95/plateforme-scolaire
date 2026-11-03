package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.plateforme.scolaire.domain.Role;
import com.plateforme.scolaire.domain.ZoneGeographique;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class RappelsListesCandidatsServiceTest {

    private static ZoneGeographique zone(long id, ZoneGeographique parent) {
        ZoneGeographique z = new ZoneGeographique();
        ReflectionTestUtils.setField(z, "id", id);
        z.setParent(parent);
        return z;
    }

    @Test
    void rolesSelonEtatEtAnciennete() {
        assertThat(RappelsListesCandidatsService.rolesAPrevenir("TRANSMISE", 3)).isEmpty();
        assertThat(RappelsListesCandidatsService.rolesAPrevenir("TRANSMISE", 7)).containsExactly(Role.DIRECTION_LOCALE);
        assertThat(RappelsListesCandidatsService.rolesAPrevenir("TRANSMISE", 14))
                .containsExactlyInAnyOrder(Role.DIRECTION_LOCALE, Role.DIRECTION_REGIONALE);
        assertThat(RappelsListesCandidatsService.rolesAPrevenir("VALIDEE_REGION", 8)).containsExactly(Role.DGECS);
        assertThat(RappelsListesCandidatsService.rolesAPrevenir("VALIDEE", 30)).isEmpty();
        assertThat(RappelsListesCandidatsService.rolesAPrevenir("BROUILLON", 30)).isEmpty();
    }

    @Test
    void responsableConcerneSiSaZoneContientLEtablissement() {
        ZoneGeographique region = zone(1, null);
        ZoneGeographique prefecture = zone(2, region);
        ZoneGeographique commune = zone(3, prefecture);
        ZoneGeographique autre = zone(9, region);
        assertThat(RappelsListesCandidatsService.concerne(Role.DIRECTION_LOCALE, commune, commune)).isTrue();
        assertThat(RappelsListesCandidatsService.concerne(Role.DIRECTION_LOCALE, prefecture, commune)).isTrue();
        assertThat(RappelsListesCandidatsService.concerne(Role.DIRECTION_REGIONALE, region, commune)).isTrue();
        assertThat(RappelsListesCandidatsService.concerne(Role.DIRECTION_LOCALE, autre, commune)).isFalse();
        assertThat(RappelsListesCandidatsService.concerne(Role.DIRECTION_LOCALE, null, commune)).isFalse();
        assertThat(RappelsListesCandidatsService.concerne(Role.DGECS, null, commune)).isTrue();
    }
}
