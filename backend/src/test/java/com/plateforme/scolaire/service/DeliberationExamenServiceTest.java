package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class DeliberationExamenServiceTest {

    @Test
    void leQuorumEstDeuxTiersDesMembresArrondiALEntierSuperieur() {
        assertThat(DeliberationExamenService.quorum(9)).isEqualTo(6);   // baccalauréat
        assertThat(DeliberationExamenService.quorum(8)).isEqualTo(6);   // CEE : 5,33 -> 6
        assertThat(DeliberationExamenService.quorum(7)).isEqualTo(5);   // BEPC : 4,67 -> 5
        assertThat(DeliberationExamenService.quorum(3)).isEqualTo(2);
    }

    @Test
    void laCompositionDuJuryDependDuNiveauDeLExamen() {
        List<String> bac = DeliberationExamenService.composition("NATIONAL");
        assertThat(bac).hasSize(9);
        assertThat(bac.get(0)).contains("président");
        assertThat(DeliberationExamenService.composition("REGION")).hasSize(7);
        assertThat(DeliberationExamenService.composition("PREFECTURE")).hasSize(8);
        assertThat(DeliberationExamenService.composition("COMMUNE")).hasSize(8);
    }
}
