package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.plateforme.scolaire.web.dto.RapportExamenDto;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExamenServiceTest {

    private static RapportExamenDto.Ligne ligne(String secteur, Integer presentes, Integer admis) {
        return new RapportExamenDto.Ligne(1L, "C", "École", secteur, null, null, presentes, admis,
                presentes == null ? null : ExamenService.taux(admis, presentes));
    }

    @Test
    void leTauxEstUnPourcentageAUneDecimale() {
        assertThat(ExamenService.taux(2, 3)).isEqualByComparingTo(new BigDecimal("66.7"));
        assertThat(ExamenService.taux(0, 10)).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void sansCandidatLeTauxEstAbsent() {
        assertThat(ExamenService.taux(0, 0)).isNull();
    }

    @Test
    void laSyntheseIgnoreLesEtablissementsSansResultats() {
        var s = ExamenService.synthese("Total", List.of(
                ligne("PUBLIC", 100, 80), ligne("PRIVE", 50, 40), ligne("PUBLIC", null, null)));

        assertThat(s.etablissements()).isEqualTo(3);
        assertThat(s.transmis()).isEqualTo(2);
        assertThat(s.presentes()).isEqualTo(150);
        assertThat(s.admis()).isEqualTo(120);
        assertThat(s.taux()).isEqualByComparingTo(new BigDecimal("80.0"));
    }
}
