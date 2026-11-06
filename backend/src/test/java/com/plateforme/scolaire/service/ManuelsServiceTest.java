package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ManuelsServiceTest {

    @Test
    void exemplairesEtDisponibles() {
        assertThat(ManuelsService.exemplaires(100, 10)).isEqualTo(90);
        assertThat(ManuelsService.exemplaires(5, 10)).isZero();
        assertThat(ManuelsService.disponibles(100, 60, 10)).isEqualTo(30);
        assertThat(ManuelsService.disponibles(10, 10, 5)).isZero();
    }

    @Test
    void couvertureEnPourcentage() {
        assertThat(ManuelsService.couverture(90, 120)).isEqualTo(75.0);
        assertThat(ManuelsService.couverture(1, 3)).isEqualTo(33.3);
        assertThat(ManuelsService.couverture(10, 0)).isNull();
    }

    @Test
    void etatSelonLaCouverture() {
        assertThat(ManuelsService.etat(100.0)).isEqualTo("COMPLET");
        assertThat(ManuelsService.etat(150.0)).isEqualTo("COMPLET");
        assertThat(ManuelsService.etat(99.9)).isEqualTo("PARTIEL");
        assertThat(ManuelsService.etat(50.0)).isEqualTo("PARTIEL");
        assertThat(ManuelsService.etat(49.9)).isEqualTo("INSUFFISANT");
        assertThat(ManuelsService.etat(null)).isEqualTo("SANS_ELEVE");
    }

    @Test
    void quantitesIncoherentes() {
        assertThat(ManuelsService.verifier(100, 60, 40)).isNull();
        assertThat(ManuelsService.verifier(100, 70, 40)).contains("dépassent");
        assertThat(ManuelsService.verifier(0, 0, 0)).isNull();
    }

    @Test
    void effectifDUnManuelDeSerieNeCompteQueCetteSerie() {
        java.util.Map<String, Integer> eff = java.util.Map.of("5-12", 90, "5-12-SCIENCES_SOCIALES", 40, "5-12-SCIENCES_MATHEMATIQUES", 50);
        org.assertj.core.api.Assertions.assertThat(ManuelsService.effectifDuManuel(eff, 5L, 12L, null)).isEqualTo(90);
        org.assertj.core.api.Assertions.assertThat(ManuelsService.effectifDuManuel(eff, 5L, 12L, "SCIENCES_SOCIALES")).isEqualTo(40);
        org.assertj.core.api.Assertions.assertThat(ManuelsService.effectifDuManuel(eff, 5L, 12L, "SCIENCES_EXPERIMENTALES")).isEqualTo(0);
    }
}
