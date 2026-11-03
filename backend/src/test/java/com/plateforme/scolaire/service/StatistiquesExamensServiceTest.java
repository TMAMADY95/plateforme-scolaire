package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.plateforme.scolaire.web.dto.StatistiquesExamensDto.Resultats;
import java.util.List;
import org.junit.jupiter.api.Test;

class StatistiquesExamensServiceTest {

    @Test
    void tauxDeReussiteArrondiAuDixieme() {
        assertThat(StatistiquesExamensService.taux(80, 100)).isEqualTo(80.0);
        assertThat(StatistiquesExamensService.taux(1, 3)).isEqualTo(33.3);
        assertThat(StatistiquesExamensService.taux(0, 0)).isNull();
    }

    @Test
    void remplissageDesSallesDExamen() {
        assertThat(StatistiquesExamensService.remplissage(60, 2)).isEqualTo(100.0);
        assertThat(StatistiquesExamensService.remplissage(45, 3)).isEqualTo(50.0);
        assertThat(StatistiquesExamensService.remplissage(10, 0)).isNull();
    }

    @Test
    void cumulDeLignesDeResultats() {
        var total = StatistiquesExamensService.cumuler("BEPC", List.of(
                new Resultats("Public", 2, 120, 100, 60, 15, 5, 60.0),
                new Resultats("Privé", 1, 50, 40, 30, 8, 2, 75.0)));
        assertThat(total.etablissements()).isEqualTo(3);
        assertThat(total.inscrits()).isEqualTo(170);
        assertThat(total.presentes()).isEqualTo(140);
        assertThat(total.admis()).isEqualTo(90);
        assertThat(total.absents()).isEqualTo(23);
        assertThat(total.elimines()).isEqualTo(7);
        assertThat(total.taux()).isEqualTo(64.3);
    }

    @Test
    void seriesDuLyceeAvecDetailParType() {
        var m = new java.util.LinkedHashMap<String, java.util.Map<String, int[]>>();
        m.put("Sciences mathématiques", java.util.Map.of("Public", new int[] {40, 20, 45}, "Privé", new int[] {10, 8, 12}));
        var series = StatistiquesExamensService.seriesDuNiveau(m);
        assertThat(series).hasSize(1);
        assertThat(series.get(0).inscrits()).isEqualTo(57);
        assertThat(series.get(0).presentes()).isEqualTo(50);
        assertThat(series.get(0).admis()).isEqualTo(28);
        assertThat(series.get(0).taux()).isEqualTo(56.0);
        assertThat(series.get(0).parType()).hasSize(2);
        assertThat(StatistiquesExamensService.seriesDuNiveau(null)).isEmpty();
        assertThat(StatistiquesExamensService.libelleSerie("SCIENCES_SOCIALES")).isEqualTo("Sciences sociales");
    }
}
