package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NumeroTelephoneTest {

    @Test
    void lesFormatsGuineensSontNormalises() {
        assertThat(NumeroTelephone.normaliser("620 00 00 00")).isEqualTo("+224620000000");
        assertThat(NumeroTelephone.normaliser("00224620000000")).isEqualTo("+224620000000");
        assertThat(NumeroTelephone.normaliser("+224 620-00-00-00")).isEqualTo("+224620000000");
        assertThat(NumeroTelephone.normaliser("224620000000")).isEqualTo("+224620000000");
    }

    @Test
    void unNumeroEtrangerAvecIndicatifEstConserve() {
        assertThat(NumeroTelephone.normaliser("+33 6 12 34 56 78")).isEqualTo("+33612345678");
    }

    @Test
    void unNumeroInutilisableDonneNull() {
        assertThat(NumeroTelephone.normaliser(null)).isNull();
        assertThat(NumeroTelephone.normaliser("")).isNull();
        assertThat(NumeroTelephone.normaliser("12345")).isNull();
    }
}
