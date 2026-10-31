package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.Test;

class SujetsExamenServiceTest {

    @Test
    void leTirageChoisitUneCandidateSelonLeHasard() {
        List<String> candidates = List.of("A", "B", "C");
        assertThat(SujetsExamenService.choisir(candidates, new Fixe(0))).isEqualTo("A");
        assertThat(SujetsExamenService.choisir(candidates, new Fixe(2))).isEqualTo("C");
    }

    @Test
    void leTirageNeSortJamaisDeLaListe() {
        List<String> reserve = List.of("B", "C");
        var hasard = RandomGenerator.of("L64X128MixRandom");
        for (int i = 0; i < 200; i++) {
            assertThat(reserve).contains(SujetsExamenService.choisir(reserve, hasard));
        }
    }

    @Test
    void troisVariantesSontRequises() {
        assertThat(SujetsExamenService.VARIANTES_REQUISES).isEqualTo(3);
        assertThat(SujetsExamenService.VARIANTES).containsExactlyInAnyOrder("A", "B", "C");
    }

    /** Générateur qui renvoie toujours le même indice. */
    private record Fixe(int valeur) implements RandomGenerator {
        @Override
        public long nextLong() {
            return valeur;
        }

        @Override
        public int nextInt(int borne) {
            return valeur;
        }
    }
}
