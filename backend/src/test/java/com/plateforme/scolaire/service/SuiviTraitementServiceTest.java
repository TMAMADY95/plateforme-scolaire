package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SuiviTraitementServiceTest {

    @Test
    void distributionDesMoyennes() {
        var t = SuiviTraitementService.tranches(new int[] {1, 2, 5, 2});
        assertThat(t).hasSize(4);
        assertThat(t.get(2).nombre()).isEqualTo(5);
        assertThat(t.get(2).pourcentage()).isEqualTo(50.0);
        assertThat(t.get(0).pourcentage()).isEqualTo(10.0);
    }

    @Test
    void distributionVide() {
        var t = SuiviTraitementService.tranches(new int[] {0, 0, 0, 0});
        assertThat(t).allMatch(x -> x.pourcentage() == null && x.nombre() == 0);
    }
}
