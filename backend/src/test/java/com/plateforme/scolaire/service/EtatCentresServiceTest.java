package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EtatCentresServiceTest {

    @Test
    void installationsEnMauvaisEtat() {
        assertThat(EtatCentresService.installationsMauvaises("BON", "MOYEN", null, "MAUVAIS")).isTrue();
        assertThat(EtatCentresService.installationsMauvaises("BON", "MOYEN", null, "BON")).isFalse();
    }

    @Test
    void centreARisque() {
        assertThat(EtatCentresService.aRisque(true, false, false, true)).isTrue();
        assertThat(EtatCentresService.aRisque(false, false, false, false)).isTrue();
        assertThat(EtatCentresService.aRisque(false, false, false, null)).isFalse();
        assertThat(EtatCentresService.aRisque(false, false, false, true)).isFalse();
    }

    @Test
    void moyenneParCentre() {
        assertThat(EtatCentresService.moyenne(100, 3)).isEqualTo(33.3);
        assertThat(EtatCentresService.moyenne(10, 0)).isNull();
    }

    @Test
    void handicapNormalise() {
        assertThat(AmenagementCandidatService.normaliser(" visuel ")).isEqualTo("VISUEL");
        assertThat(AmenagementCandidatService.normaliser("  ")).isNull();
        assertThat(AmenagementCandidatService.normaliser(null)).isNull();
        assertThat(AmenagementCandidatService.HANDICAPS).contains("MOTEUR", "AUDITIF");
    }
}
