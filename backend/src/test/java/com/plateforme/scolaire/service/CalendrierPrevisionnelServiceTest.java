package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class CalendrierPrevisionnelServiceTest {

    private static final LocalDate J = LocalDate.of(2026, 7, 10);

    @Test
    void ordreValide() {
        assertThatCode(() -> CalendrierPrevisionnelService.verifierOrdre(J, J.plusDays(2), J.plusDays(10), J.plusDays(15), J.plusDays(20)))
                .doesNotThrowAnyException();
        assertThatCode(() -> CalendrierPrevisionnelService.verifierOrdre(null, null, null, null, null)).doesNotThrowAnyException();
    }

    @Test
    void correctionAvantLaFinDesEpreuvesRefusee() {
        assertThatThrownBy(() -> CalendrierPrevisionnelService.verifierOrdre(J, J.minusDays(1), null, null, null))
                .hasMessageContaining("précède");
    }

    @Test
    void proclamationAvantDeliberationRefusee() {
        assertThatThrownBy(() -> CalendrierPrevisionnelService.verifierOrdre(null, null, null, J.plusDays(5), J.plusDays(3)))
                .hasMessageContaining("proclamation");
    }

    @Test
    void lesDatesVidesSontIgnoreesDansLaChaine() {
        assertThatThrownBy(() -> CalendrierPrevisionnelService.verifierOrdre(J, null, null, null, J.minusDays(1)))
                .hasMessageContaining("fin des épreuves");
    }

    @Test
    void retards() {
        LocalDate auj = J.plusDays(30);
        assertThat(CalendrierPrevisionnelService.retards(auj, "EN_COURS", J.plusDays(10), J.plusDays(15), J.plusDays(20)))
                .containsExactly("CORRECTION", "DELIBERATION", "PROCLAMATION");
        assertThat(CalendrierPrevisionnelService.retards(auj, "DELIBERE", J.plusDays(10), J.plusDays(15), J.plusDays(20)))
                .containsExactly("PROCLAMATION");
        assertThat(CalendrierPrevisionnelService.retards(auj, "PROCLAME", J.plusDays(10), J.plusDays(15), J.plusDays(20))).isEmpty();
        assertThat(CalendrierPrevisionnelService.retards(J, "EN_COURS", J.plusDays(10), null, null)).isEmpty();
    }
}
