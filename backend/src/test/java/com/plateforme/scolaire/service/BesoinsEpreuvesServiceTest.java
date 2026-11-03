package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class BesoinsEpreuvesServiceTest {

    @Test
    void picDesBesoins() {
        assertThat(BesoinsEpreuvesService.pic(Arrays.asList(3, null, 10, 7))).isEqualTo(10);
        assertThat(BesoinsEpreuvesService.pic(Arrays.asList(null, null))).isNull();
        assertThat(BesoinsEpreuvesService.pic(List.of())).isNull();
    }

    @Test
    void manqueSurLePic() {
        assertThat(PersonnelNationalService.manque(10, 4)).isEqualTo(6);
        assertThat(PersonnelNationalService.manque(3, 8)).isZero();
        assertThat(PersonnelNationalService.manque(null, 8)).isNull();
    }
}
