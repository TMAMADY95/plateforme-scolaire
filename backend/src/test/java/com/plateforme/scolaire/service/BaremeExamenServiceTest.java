package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BaremeExamenServiceTest {

    @Test
    void seulsLes10EtLes20SontValides() {
        assertThat(BaremeExamenService.valide(10)).isTrue();
        assertThat(BaremeExamenService.valide(20)).isTrue();
        assertThat(BaremeExamenService.valide(15)).isFalse();
        assertThat(BaremeExamenService.valide(0)).isFalse();
    }
}
