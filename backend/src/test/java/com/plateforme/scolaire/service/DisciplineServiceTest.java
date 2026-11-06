package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class DisciplineServiceTest {

    private static final LocalDate DEBUT = LocalDate.of(2026, 11, 2);

    @Test
    void sanctionsSelonLaGravite() {
        assertThat(DisciplineService.sanctionAutorisee("LEGERE", "AVERTISSEMENT")).isTrue();
        assertThat(DisciplineService.sanctionAutorisee("LEGERE", "EXCLUSION_TEMPORAIRE")).isFalse();
        assertThat(DisciplineService.sanctionAutorisee("GRAVE", "EXCLUSION_TEMPORAIRE")).isTrue();
        assertThat(DisciplineService.sanctionAutorisee("GRAVE", "EXCLUSION_DEFINITIVE")).isFalse();
        assertThat(DisciplineService.sanctionAutorisee("TRES_GRAVE", "EXCLUSION_DEFINITIVE")).isTrue();
        assertThat(DisciplineService.sanctionAutorisee("TRES_GRAVE", "AVERTISSEMENT")).isFalse();
        assertThat(DisciplineService.sanctionAutorisee("INCONNUE", "BLAME")).isFalse();
    }

    @Test
    void exclusionTemporaireExigeUneDureeEtUneDate() {
        assertThat(DisciplineService.verifierSanction("GRAVE", "EXCLUSION_TEMPORAIRE", 3, DEBUT, null)).isNull();
        assertThat(DisciplineService.verifierSanction("GRAVE", "EXCLUSION_TEMPORAIRE", null, DEBUT, null)).contains("dure de 1 à 8 jours");
        assertThat(DisciplineService.verifierSanction("GRAVE", "EXCLUSION_TEMPORAIRE", 9, DEBUT, null)).contains("dure de 1 à 8 jours");
        assertThat(DisciplineService.verifierSanction("GRAVE", "EXCLUSION_TEMPORAIRE", 3, null, null)).contains("date de début");
    }

    @Test
    void dureeInterditeHorsExclusionTemporaire() {
        assertThat(DisciplineService.verifierSanction("LEGERE", "BLAME", 2, null, null)).contains("ne concernent que");
        assertThat(DisciplineService.verifierSanction("LEGERE", "BLAME", null, null, null)).isNull();
    }

    @Test
    void observationObligatoireEnCasDeSanctionLourde() {
        assertThat(DisciplineService.verifierSanction("TRES_GRAVE", "EXCLUSION_DEFINITIVE", null, null, "  ")).contains("observation");
        assertThat(DisciplineService.verifierSanction("TRES_GRAVE", "EXCLUSION_DEFINITIVE", null, null, "Violence sur un enseignant")).isNull();
        assertThat(DisciplineService.verifierSanction("GRAVE", "CONSEIL_DISCIPLINE", null, null, null)).contains("observation");
    }

    @Test
    void sanctionInterditeRemontee() {
        assertThat(DisciplineService.verifierSanction("LEGERE", "EXCLUSION_DEFINITIVE", null, null, "x")).contains("n'est pas permise");
    }

    @Test
    void recidiveADroiteDeTroisIncidents() {
        assertThat(DisciplineService.recidive(2)).isFalse();
        assertThat(DisciplineService.recidive(3)).isTrue();
    }
}
