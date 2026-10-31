package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class PersonnelCentreServiceTest {

    @Test
    void ilFautDeuxSurveillantsParSalleEtLesFonctionsDeBase() {
        var besoins = PersonnelCentreService.besoins(2, List.of(
                new PersonnelCentreService.Affectation("PRESIDENT", null),
                new PersonnelCentreService.Affectation("SURVEILLANT", 1),
                new PersonnelCentreService.Affectation("SURVEILLANT", 1),
                new PersonnelCentreService.Affectation("SURVEILLANT", 2)));

        assertThat(besoins).hasSize(5);   // président, chef de centre, secrétariat, 2 salles
        assertThat(besoins.get(0).satisfait()).isTrue();
        assertThat(besoins.get(1).satisfait()).isFalse();
        assertThat(besoins.get(3).satisfait()).isTrue();    // salle 1 : 2 surveillants
        assertThat(besoins.get(4).satisfait()).isFalse();   // salle 2 : 1 seul
    }

    @Test
    void lesSurveillantsDUneAutreSalleNeCompentPasPourLaSalle() {
        var besoins = PersonnelCentreService.besoins(1, List.of(
                new PersonnelCentreService.Affectation("SURVEILLANT", 2), new PersonnelCentreService.Affectation("SURVEILLANT", 2)));
        assertThat(besoins.get(3).affectes()).isZero();
    }
}
