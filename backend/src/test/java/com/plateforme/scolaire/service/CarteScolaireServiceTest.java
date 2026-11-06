package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CarteScolaireServiceTest {

    @Test
    void necessaireArrondiAuSuperieur() {
        assertThat(CarteScolaireService.necessaire(0, 50)).isEqualTo(0);
        assertThat(CarteScolaireService.necessaire(50, 50)).isEqualTo(1);
        assertThat(CarteScolaireService.necessaire(51, 50)).isEqualTo(2);
        assertThat(CarteScolaireService.necessaire(420, 40)).isEqualTo(11);
    }

    @Test
    void manqueJamaisNegatif() {
        assertThat(CarteScolaireService.manque(11, 8)).isEqualTo(3);
        assertThat(CarteScolaireService.manque(5, 8)).isEqualTo(0);
    }
}
