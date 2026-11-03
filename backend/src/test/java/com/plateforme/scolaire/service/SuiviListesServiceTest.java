package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.plateforme.scolaire.domain.ListeCandidats;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class SuiviListesServiceTest {

    @Test
    void quiDoitAgir() {
        assertThat(SuiviListesService.enAttenteDe("TRANSMISE")).isEqualTo("VERIFICATION");
        assertThat(SuiviListesService.enAttenteDe("VALIDEE_REGION")).isEqualTo("VALIDATION");
        assertThat(SuiviListesService.enAttenteDe("BROUILLON")).isNull();
        assertThat(SuiviListesService.enAttenteDe("VALIDEE")).isNull();
    }

    @Test
    void joursAttente() {
        Instant depuis = Instant.parse("2026-10-01T08:00:00Z");
        Instant maintenant = Instant.parse("2026-10-11T09:00:00Z");
        assertThat(SuiviListesService.joursAttente("TRANSMISE", depuis, maintenant)).isEqualTo(10L);
        assertThat(SuiviListesService.joursAttente("VALIDEE", depuis, maintenant)).isNull();
        assertThat(SuiviListesService.joursAttente("TRANSMISE", null, maintenant)).isNull();
    }

    @Test
    void dateDeDebutSelonLEtape() {
        ListeCandidats l = new ListeCandidats();
        Instant t = Instant.parse("2026-10-01T08:00:00Z");
        Instant r = Instant.parse("2026-10-05T08:00:00Z");
        ReflectionTestUtils.setField(l, "transmiseLe", t);
        ReflectionTestUtils.setField(l, "valideeRegionLe", r);
        ReflectionTestUtils.setField(l, "statut", "TRANSMISE");
        assertThat(SuiviListesService.depuis(l)).isEqualTo(t);
        ReflectionTestUtils.setField(l, "statut", "VALIDEE_REGION");
        assertThat(SuiviListesService.depuis(l)).isEqualTo(r);
        ReflectionTestUtils.setField(l, "statut", "BROUILLON");
        assertThat(SuiviListesService.depuis(l)).isNull();
    }
}
