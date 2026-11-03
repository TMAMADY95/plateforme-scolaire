package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PersonnelNationalServiceTest {

    @Test
    void personnesManquantes() {
        assertThat(PersonnelNationalService.manque(10, 7)).isEqualTo(3);
        assertThat(PersonnelNationalService.manque(10, 12)).isZero();
        assertThat(PersonnelNationalService.manque(null, 5)).isNull();
    }

    @Test
    void fonctionsDistinctes() {
        assertThat(PersonnelNationalService.FONCTIONS_NATIONALES).containsExactlyInAnyOrder("JURY", "SUPERVISEUR", "INSPECTEUR", "INFORMATIQUE");
        assertThat(PersonnelNationalService.BESOINS_DECLARABLES).contains("CORRECTEUR", "SECURITE").doesNotContain("SURVEILLANT", "CHEF_CENTRE");
    }
}
