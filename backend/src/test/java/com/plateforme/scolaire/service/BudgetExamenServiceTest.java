package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.plateforme.scolaire.domain.BudgetExamen;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BudgetExamenServiceTest {

    private static BudgetExamen b(String rubrique, long prevu, long engage, long paye) {
        var x = new BudgetExamen();
        x.setRubrique(rubrique);
        x.setMontantPrevu(prevu);
        x.setMontantEngage(engage);
        x.setMontantPaye(paye);
        return x;
    }

    @Test
    void coutsUnitaires() {
        assertThat(BudgetExamenService.coutMoyen(1_000_000, 3)).isEqualTo(333_333L);
        assertThat(BudgetExamenService.coutMoyen(1_000_000, 0)).isNull();
    }

    @Test
    void budgetAvecTotauxEtEcarts() {
        var d = BudgetExamenService.assembler(1L, 100, 4, Map.of(
                "IMPRESSION", b("IMPRESSION", 1000, 1200, 800),
                "TRANSPORT", b("TRANSPORT", 500, 400, 400)));
        assertThat(d.lignes()).hasSize(8);
        assertThat(d.total().prevu()).isEqualTo(1500);
        assertThat(d.total().engage()).isEqualTo(1600);
        assertThat(d.total().paye()).isEqualTo(1200);
        assertThat(d.total().ecart()).isEqualTo(100);
        assertThat(d.lignes().get(0).ecart()).isEqualTo(200);
        assertThat(d.tauxEngagement()).isEqualTo(106.7);
        assertThat(d.tauxPaiement()).isEqualTo(75.0);
        assertThat(d.coutParCandidat()).isEqualTo(16L);
        assertThat(d.coutParCentre()).isEqualTo(400L);
    }

    @Test
    void sansBudgetLesTauxSontVides() {
        var d = BudgetExamenService.assembler(1L, 0, 0, Map.of());
        assertThat(d.tauxEngagement()).isNull();
        assertThat(d.coutParCandidat()).isNull();
    }

    @Test
    void delaiMoyenDeProclamation() {
        assertThat(IndicateursDgecsService.delaiMoyen(List.of(10L, 20L))).isEqualTo(15);
        assertThat(IndicateursDgecsService.delaiMoyen(List.of())).isNull();
    }
}
