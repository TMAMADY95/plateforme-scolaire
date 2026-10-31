package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NotesExamenServiceTest {

    @Test
    void laMoyenneEstPondereeParLesCoefficients() {
        // (12x3 + 14x4 + 10x2 + 13x2) / 11 = 138 / 11 = 12,545 -> 12,55
        var coef = Map.of(1L, 3, 2L, 4, 3L, 2, 4L, 2);
        var notes = Map.of(1L, new BigDecimal("12"), 2L, new BigDecimal("14"), 3L, new BigDecimal("10"), 4L, new BigDecimal("13"));
        assertThat(NotesExamenService.moyenne(coef, notes)).isEqualByComparingTo(new BigDecimal("12.55"));
    }

    @Test
    void sansToutesLesNotesIlNYAPasDeMoyenne() {
        assertThat(NotesExamenService.moyenne(Map.of(1L, 1, 2L, 1), Map.of(1L, new BigDecimal("15")))).isNull();
        assertThat(NotesExamenService.moyenne(Map.of(), Map.of())).isNull();
    }

    @Test
    void lAdmissionSeJoueADixDeMoyenneEtLaFraudeDisqualifie() {
        assertThat(NotesExamenService.decision("PRESENT", new BigDecimal("10.00"))).isEqualTo("ADMIS");
        assertThat(NotesExamenService.decision("PRESENT", new BigDecimal("9.99"))).isEqualTo("NON_ADMIS");
        assertThat(NotesExamenService.decision("PRESENT", null)).isNull();
        assertThat(NotesExamenService.decision("ABSENT", null)).isNull();
        assertThat(NotesExamenService.decision("ELIMINE", null)).isEqualTo("NON_ADMIS");
    }
}
