package com.plateforme.scolaire.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class BaremeExamenTest {

    private static String m(String moyenne, int bareme) {
        return BaremeExamen.mention(new BigDecimal(moyenne), bareme);
    }

    @Test
    void mentionsSur20() {
        assertThat(m("10.00", 20)).isEqualTo("PASSABLE");
        assertThat(m("11.99", 20)).isEqualTo("PASSABLE");
        assertThat(m("12.00", 20)).isEqualTo("ASSEZ_BIEN");
        assertThat(m("13.99", 20)).isEqualTo("ASSEZ_BIEN");
        assertThat(m("14.00", 20)).isEqualTo("BIEN");
        assertThat(m("15.99", 20)).isEqualTo("BIEN");
        assertThat(m("16.00", 20)).isEqualTo("TRES_BIEN");
    }

    @Test
    void mentionsSur10Primaire() {
        assertThat(m("5.00", 10)).isEqualTo("PASSABLE");
        assertThat(m("5.99", 10)).isEqualTo("PASSABLE");
        assertThat(m("6.00", 10)).isEqualTo("ASSEZ_BIEN");
        assertThat(m("6.99", 10)).isEqualTo("ASSEZ_BIEN");
        assertThat(m("7.00", 10)).isEqualTo("BIEN");
        assertThat(m("7.99", 10)).isEqualTo("BIEN");
        assertThat(m("8.00", 10)).isEqualTo("TRES_BIEN");
        assertThat(m("10.00", 10)).isEqualTo("TRES_BIEN");
    }

    @Test
    void seuilDAdmissionEtBareme() {
        assertThat(BaremeExamen.seuilAdmission(10)).isEqualByComparingTo("5");
        assertThat(BaremeExamen.seuilAdmission(20)).isEqualByComparingTo("10");
        assertThat(BaremeExamen.pour("PRIMAIRE")).isEqualTo(10);
        assertThat(BaremeExamen.pour("COLLEGE")).isEqualTo(20);
        assertThat(BaremeExamen.pour(null)).isEqualTo(20);
    }
}
