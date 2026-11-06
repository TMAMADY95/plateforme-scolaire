package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.plateforme.scolaire.domain.AnneeScolaire;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class RentreeServiceTest {

    private static AnneeScolaire annee(String libelle, int debut) {
        AnneeScolaire a = new AnneeScolaire();
        a.setLibelle(libelle);
        a.setDateDebut(LocalDate.of(debut, 10, 1));
        a.setDateFin(LocalDate.of(debut + 1, 7, 15));
        return a;
    }

    @Test
    void classementDesDecisions() {
        assertThat(RentreeService.classer("PASSAGE", 5, 12)).isEqualTo("A_REINSCRIRE");
        assertThat(RentreeService.classer("REDOUBLEMENT", 12, 12)).isEqualTo("A_REINSCRIRE");
        assertThat(RentreeService.classer("PASSAGE", 12, 12)).isEqualTo("FIN_DE_PARCOURS");
        assertThat(RentreeService.classer("EXCLUSION", 3, 12)).isEqualTo("EXCLU");
        assertThat(RentreeService.classer(null, 3, 12)).isEqualTo("SANS_DECISION");
    }

    @Test
    void anneeSuivante() {
        AnneeScolaire a25 = annee("2025-2026", 2025);
        AnneeScolaire a26 = annee("2026-2027", 2026);
        AnneeScolaire a27 = annee("2027-2028", 2027);
        assertThat(RentreeService.suivante(List.of(a27, a26, a25), a25)).isSameAs(a26);
        assertThat(RentreeService.suivante(List.of(a27, a26, a25), a27)).isNull();
    }
}
