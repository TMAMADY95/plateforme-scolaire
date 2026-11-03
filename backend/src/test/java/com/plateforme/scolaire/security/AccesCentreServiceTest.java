package com.plateforme.scolaire.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.plateforme.scolaire.domain.CentreExamen;
import com.plateforme.scolaire.domain.Etablissement;
import com.plateforme.scolaire.domain.ZoneGeographique;
import org.junit.jupiter.api.Test;

class AccesCentreServiceTest {

    private static ZoneGeographique zone(Long id, ZoneGeographique parent) {
        ZoneGeographique z = new ZoneGeographique();
        z.setId(id);
        z.setParent(parent);
        return z;
    }

    @Test
    void uneZoneEstSousElleMemeEtSousSesParents() {
        ZoneGeographique region = zone(1L, null);
        ZoneGeographique prefecture = zone(2L, region);
        ZoneGeographique commune = zone(3L, prefecture);

        assertThat(AccesCentreService.zoneSousZone(commune, 3L)).isTrue();
        assertThat(AccesCentreService.zoneSousZone(commune, 2L)).isTrue();
        assertThat(AccesCentreService.zoneSousZone(commune, 1L)).isTrue();
        assertThat(AccesCentreService.zoneSousZone(prefecture, 3L)).isFalse();
        assertThat(AccesCentreService.zoneSousZone(null, 1L)).isFalse();
    }

    @Test
    void laZoneDuCentreDecideSinonCelleDeSonEtablissement() {
        ZoneGeographique region = zone(1L, null);
        ZoneGeographique prefecture = zone(2L, region);
        ZoneGeographique autre = zone(9L, null);

        CentreExamen avecZone = new CentreExamen();
        avecZone.setZone(prefecture);
        assertThat(AccesCentreService.centreDansZone(avecZone, 1L)).isTrue();
        assertThat(AccesCentreService.centreDansZone(avecZone, 9L)).isFalse();

        Etablissement ecole = new Etablissement();
        ecole.setZone(autre);
        CentreExamen avecEcole = new CentreExamen();
        avecEcole.setEtablissement(ecole);
        assertThat(AccesCentreService.centreDansZone(avecEcole, 9L)).isTrue();
        assertThat(AccesCentreService.centreDansZone(avecEcole, 1L)).isFalse();

        assertThat(AccesCentreService.centreDansZone(new CentreExamen(), 1L)).isFalse();
        assertThat(AccesCentreService.centreDansZone(avecZone, null)).isFalse();
    }
}
