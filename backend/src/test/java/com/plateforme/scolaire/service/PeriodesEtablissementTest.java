package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.plateforme.scolaire.domain.Etablissement;
import com.plateforme.scolaire.domain.Periode;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class PeriodesEtablissementTest {

    private static Periode periode(long id, int ordre, Long etablissementId) {
        Periode p = new Periode();
        p.setId(id);
        p.setOrdre(ordre);
        if (etablissementId != null) {
            Etablissement e = new Etablissement();
            e.setId(etablissementId);
            p.setEtablissement(e);
        }
        return p;
    }

    @Test
    void ledecoupageEnTrimestresCouvreToutLAnneeSansTrouNiChevauchement() {
        List<Periode> t = PeriodesEtablissementService.decouper(LocalDate.of(2026, 9, 1), LocalDate.of(2027, 6, 30), 3, "Trimestre");

        assertThat(t).hasSize(3);
        assertThat(t.get(0).getDateDebut()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(t.get(2).getDateFin()).isEqualTo(LocalDate.of(2027, 6, 30));
        for (int i = 1; i < t.size(); i++) {
            assertThat(t.get(i).getDateDebut()).isEqualTo(t.get(i - 1).getDateFin().plusDays(1));
        }
        assertThat(t.get(1).getLibelle()).isEqualTo("Trimestre 2");
    }

    @Test
    void ledecoupageEnSemestresDonneDeuxPeriodes() {
        List<Periode> s = PeriodesEtablissementService.decouper(LocalDate.of(2026, 10, 1), LocalDate.of(2027, 6, 30), 2, "Semestre");

        assertThat(s).extracting(Periode::getOrdre).containsExactly(1, 2);
        assertThat(s.get(1).getDateFin()).isEqualTo(LocalDate.of(2027, 6, 30));
    }

    @Test
    void lesPeriodesPropresDeLEtablissementPrennentLePasSurLesNationales() {
        List<Periode> toutes = List.of(periode(1, 1, null), periode(2, 2, null), periode(3, 1, 7L), periode(4, 2, 7L));

        assertThat(PeriodesApplicables.pour(toutes, 7L)).extracting(Periode::getId).containsExactly(3L, 4L);
        assertThat(PeriodesApplicables.pour(toutes, 8L)).extracting(Periode::getId).containsExactly(1L, 2L);
        assertThat(PeriodesApplicables.pour(toutes, null)).extracting(Periode::getId).containsExactly(1L, 2L);
    }
}
