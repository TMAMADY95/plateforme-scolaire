package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class SituationStatistiqueServiceTest {

    @Test
    void situationDefinitiveSeulementSiToutEstProclame() {
        assertThat(SituationStatistiqueService.peutEtreDefinitive(List.of("PROCLAME", "PROCLAME"))).isTrue();
        assertThat(SituationStatistiqueService.peutEtreDefinitive(List.of("PROCLAME", "EN_COURS"))).isFalse();
        assertThat(SituationStatistiqueService.peutEtreDefinitive(List.of())).isFalse();
    }
}
