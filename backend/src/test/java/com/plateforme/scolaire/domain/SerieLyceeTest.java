package com.plateforme.scolaire.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SerieLyceeTest {

    @Test
    void normaliseLesCodes() {
        assertThat(SerieLycee.normaliser(null)).isNull();
        assertThat(SerieLycee.normaliser("  ")).isNull();
        assertThat(SerieLycee.normaliser(" SCIENCES_SOCIALES ")).isEqualTo("SCIENCES_SOCIALES");
        assertThatThrownBy(() -> SerieLycee.normaliser("LETTRES")).isInstanceOf(IllegalArgumentException.class);
    }
}
