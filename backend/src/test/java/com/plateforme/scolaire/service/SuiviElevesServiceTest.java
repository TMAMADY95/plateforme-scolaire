package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class SuiviElevesServiceTest {

    private static final LocalDate DEBUT = LocalDate.of(2026, 10, 1);
    private static final LocalDate FIN = LocalDate.of(2027, 7, 15);

    @Test
    void tauxPourCentUneDecimale() {
        assertThat(SuiviElevesService.taux(3, 200)).isEqualTo(1.5);
        assertThat(SuiviElevesService.taux(1, 3)).isEqualTo(33.3);
        assertThat(SuiviElevesService.taux(0, 0)).isNull();
    }

    @Test
    void dateDAbandonDansLAnneeEtPasDansLeFutur() {
        LocalDate aujourdhui = LocalDate.of(2026, 11, 5);
        assertThat(SuiviElevesService.verifierDate(LocalDate.of(2026, 11, 2), DEBUT, FIN, aujourdhui)).isNull();
        assertThat(SuiviElevesService.verifierDate(LocalDate.of(2026, 9, 1), DEBUT, FIN, aujourdhui)).isNotNull();
        assertThat(SuiviElevesService.verifierDate(LocalDate.of(2027, 8, 1), DEBUT, FIN, aujourdhui)).isNotNull();
        assertThat(SuiviElevesService.verifierDate(LocalDate.of(2026, 12, 1), DEBUT, FIN, aujourdhui)).isNotNull();
    }
}
