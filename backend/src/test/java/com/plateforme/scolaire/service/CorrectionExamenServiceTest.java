package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class CorrectionExamenServiceTest {

    @Test
    void lIntituleDependDuNiveauDeLExamen() {
        assertThat(CorrectionExamenService.niveau("NATIONAL")).contains("nationale");
        assertThat(CorrectionExamenService.niveau("REGION")).contains("régionale");
        assertThat(CorrectionExamenService.niveau("PREFECTURE")).contains("préfectorale");
        assertThat(CorrectionExamenService.niveau(null)).contains("communale");
    }

    @Test
    void uneCommissionVideReclamePresidentSecretaireEtDeuxCorrecteurs() {
        assertThat(CorrectionExamenService.manques(List.of())).containsExactly("président", "secrétaire", "2 correcteur(s)");
    }

    @Test
    void uneCommissionCompleteNeManqueDeRien() {
        assertThat(CorrectionExamenService.manques(List.of("PRESIDENT", "SECRETAIRE", "CORRECTEUR", "CORRECTEUR"))).isEmpty();
        assertThat(CorrectionExamenService.manques(List.of("PRESIDENT", "SECRETAIRE", "CORRECTEUR"))).containsExactly("1 correcteur(s)");
    }
}
