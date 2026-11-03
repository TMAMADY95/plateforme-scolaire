package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RectificationNoteServiceTest {

    @Test
    void variationDuNombreDAdmis() {
        assertThat(RectificationNoteService.variationAdmis("ADMIS", "NON_ADMIS")).isEqualTo(-1);
        assertThat(RectificationNoteService.variationAdmis("NON_ADMIS", "ADMIS")).isEqualTo(1);
        assertThat(RectificationNoteService.variationAdmis("ADMIS", "ADMIS")).isZero();
        assertThat(RectificationNoteService.variationAdmis("NON_ADMIS", "NON_ADMIS")).isZero();
        assertThat(RectificationNoteService.variationAdmis(null, "NON_ADMIS")).isZero();
        assertThat(RectificationNoteService.variationAdmis(null, "ADMIS")).isEqualTo(1);
    }
}
