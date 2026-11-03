package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class VerificationsDiplomeServiceTest {

    @Test
    void codeNormalise() {
        assertThat(VerificationsDiplomeService.normaliser("  ab12cd ")).isEqualTo("AB12CD");
        assertThat(VerificationsDiplomeService.normaliser(null)).isEmpty();
        assertThat(VerificationsDiplomeService.normaliser("x".repeat(50))).hasSize(20);
    }

    @Test
    void tentativeSuspecte() {
        assertThat(VerificationsDiplomeService.suspect(2)).isFalse();
        assertThat(VerificationsDiplomeService.suspect(3)).isTrue();
    }

    @Test
    void motifDeRevocationStable() {
        assertThat(RectificationNoteService.motifRevocation(12L)).isEqualTo("Rectification de note (réclamation n° 12)");
        assertThat(RectificationNoteService.variationAdmis("ADMIS", "NON_ADMIS")).isEqualTo(-1);
        assertThat(RectificationNoteService.variationAdmis("NON_ADMIS", "ADMIS")).isEqualTo(1);
    }
}
