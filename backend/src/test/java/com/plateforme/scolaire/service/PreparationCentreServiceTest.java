package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class PreparationCentreServiceTest {

    @Test
    void leReglementPrevoitSeptTravaux() {
        assertThat(PreparationCentreService.ELEMENTS.keySet()).containsExactly(
                "SALLES", "TABLES_BANCS", "AFFICHAGE", "SECRETARIAT", "SECURITE", "PERSONNEL", "MATERIEL");
    }

    @Test
    void unCentreNEstPretQueQuandLesSeptTravauxSontFaits() {
        assertThat(PreparationCentreService.pret(List.of())).isFalse();
        assertThat(PreparationCentreService.pret(List.of("SALLES", "TABLES_BANCS", "AFFICHAGE", "SECRETARIAT", "SECURITE", "PERSONNEL")))
                .isFalse();
        assertThat(PreparationCentreService.pret(List.of("SALLES", "TABLES_BANCS", "AFFICHAGE", "SECRETARIAT", "SECURITE", "PERSONNEL", "MATERIEL")))
                .isTrue();
    }
}
