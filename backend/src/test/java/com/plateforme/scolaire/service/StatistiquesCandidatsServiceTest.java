package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.plateforme.scolaire.service.StatistiquesCandidatsService.Candidat;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class StatistiquesCandidatsServiceTest {

    private static Candidat c(String niveau, String sexe, boolean libre, String statut, String decision, String moyenne, String zone) {
        return new Candidat(niveau, sexe, libre, statut, decision, moyenne == null ? null : new BigDecimal(moyenne), 20, zone, "REGION",
                zone.equals("Conakry") ? "Conakry" : "Kindia", zone.equals("Conakry") ? "Conakry" : "Kindia (préfecture)");
    }

    @Test
    void pourcentagesEtEvolution() {
        assertThat(StatistiquesCandidatsService.pct(1, 3)).isEqualTo(33.3);
        assertThat(StatistiquesCandidatsService.pct(1, 0)).isNull();
        assertThat(StatistiquesCandidatsService.evolution(120, 100)).isEqualTo(20.0);
        assertThat(StatistiquesCandidatsService.evolution(80, 100)).isEqualTo(-20.0);
        assertThat(StatistiquesCandidatsService.evolution(80, null)).isNull();
        assertThat(StatistiquesCandidatsService.evolution(80, 0)).isNull();
    }

    @Test
    void profilParticipationEtReussite() {
        var r = StatistiquesCandidatsService.assembler(2L, 1L, List.of(
                c("BAC", "F", false, "PRESENT", "ADMIS", "12", "Conakry"),
                c("BAC", "F", true, "PRESENT", "NON_ADMIS", "8", "Conakry"),
                c("BAC", "M", false, "PRESENT", "ADMIS", "15", "Kindia"),
                c("BAC", "M", false, "ABSENT", null, null, "Kindia"),
                c("BEPC", "M", false, "ELIMINE", "NON_ADMIS", null, "Kindia")),
                List.of(c("BAC", "F", false, "PRESENT", "ADMIS", "11", "Conakry"), c("BAC", "M", false, "PRESENT", "NON_ADMIS", "7", "Conakry")));
        var t = r.total();
        assertThat(t.inscrits()).isEqualTo(5);
        assertThat(t.filles()).isEqualTo(2);
        assertThat(t.garcons()).isEqualTo(3);
        assertThat(t.libres()).isEqualTo(1);
        assertThat(t.presents()).isEqualTo(3);
        assertThat(t.absents()).isEqualTo(1);
        assertThat(t.elimines()).isEqualTo(1);
        assertThat(t.admis()).isEqualTo(2);
        assertThat(t.tauxReussite()).isEqualTo(66.7);
        assertThat(t.tauxReussiteInscrits()).isEqualTo(40.0);
        assertThat(t.tauxReussiteFilles()).isEqualTo(50.0);
        assertThat(t.tauxReussiteGarcons()).isEqualTo(100.0);
        assertThat(t.tauxAbsence()).isEqualTo(20.0);
        var bac = r.parNiveau().stream().filter(l -> l.libelle().equals("BAC")).findFirst().orElseThrow();
        assertThat(bac.inscrits()).isEqualTo(4);
        assertThat(bac.inscritsPrecedent()).isEqualTo(2);
        assertThat(bac.evolutionInscrits()).isEqualTo(100.0);
        assertThat(bac.tauxReussitePrecedent()).isEqualTo(50.0);
        assertThat(r.parZone().get(0).inscrits()).isGreaterThanOrEqualTo(r.parZone().get(1).inscrits());
    }

    @Test
    void mentionsDesAdmisToujoursLesQuatre() {
        var m = StatistiquesCandidatsService.mentions(List.of(
                c("BAC", "F", false, "PRESENT", "ADMIS", "10", "A"), c("BAC", "F", false, "PRESENT", "ADMIS", "16.5", "A"),
                c("BAC", "F", false, "PRESENT", "NON_ADMIS", "5", "A")));
        assertThat(m).extracting("mention").containsExactly("TRES_BIEN", "BIEN", "ASSEZ_BIEN", "PASSABLE");
        assertThat(m.get(0).nombre()).isEqualTo(1);
        assertThat(m.get(3).nombre()).isEqualTo(1);
        assertThat(m.get(0).pourcentage()).isEqualTo(50.0);
    }

    @Test
    void ancetreParType() {
        var commune = new String[] {"Ratoma", "COMMUNE"};
        var pref = new String[] {"Conakry", "PREFECTURE"};
        var region = new String[] {"Conakry (région)", "REGION"};
        assertThat(StatistiquesCandidatsService.ancetre("REGION", commune, pref, region)).isEqualTo("Conakry (région)");
        assertThat(StatistiquesCandidatsService.ancetre("PREFECTURE", commune, pref, region)).isEqualTo("Conakry");
        assertThat(StatistiquesCandidatsService.ancetre("COMMUNE", commune, pref, region)).isEqualTo("Ratoma");
        assertThat(StatistiquesCandidatsService.ancetre("REGION", commune, null, null)).isNull();
        assertThat(StatistiquesCandidatsService.ancetre("REGION", region, null, null)).isEqualTo("Conakry (région)");
    }

    @Test
    void repartitionParRegionEtPrefecture() {
        var r = StatistiquesCandidatsService.assembler(2L, null, List.of(
                c("BAC", "F", false, "PRESENT", "ADMIS", "12", "Conakry"), c("BAC", "M", false, "PRESENT", "ADMIS", "12", "Kindia"),
                c("BAC", "M", false, "ABSENT", null, null, "Kindia")), List.of());
        assertThat(r.parRegion()).extracting("libelle").containsExactly("Conakry", "Kindia");
        assertThat(r.parPrefecture()).hasSize(2);
        assertThat(r.parRegion().get(1).inscrits()).isEqualTo(2);
    }

    @Test
    void sansAnneePrecedenteLaComparaisonEstVide() {
        var r = StatistiquesCandidatsService.assembler(1L, null, List.of(c("BAC", "F", false, "PRESENT", "ADMIS", "12", "A")), List.of());
        assertThat(r.parNiveau().get(0).inscritsPrecedent()).isNull();
        assertThat(r.parNiveau().get(0).evolutionInscrits()).isNull();
    }
}
