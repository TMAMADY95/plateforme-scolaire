package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ConseilClasseServiceTest {

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    @Test
    void moyenneAnnuelleIgnoreLesPeriodesSansNote() {
        assertThat(ConseilClasseService.moyenneAnnuelle(Arrays.asList(bd("12.00"), bd("9.50"), null))).isEqualByComparingTo("10.75");
        assertThat(ConseilClasseService.moyenneAnnuelle(Arrays.asList(null, null))).isNull();
        assertThat(ConseilClasseService.moyenneAnnuelle(List.of())).isNull();
        assertThat(ConseilClasseService.moyenneAnnuelle(List.of(bd("10"), bd("11"), bd("11")))).isEqualByComparingTo("10.67");
    }

    @Test
    void suggestionSelonLesSeuils() {
        assertThat(ConseilClasseService.suggestion(bd("10.00"))).isEqualTo("ADMIS");
        assertThat(ConseilClasseService.suggestion(bd("9.99"))).isEqualTo("A_DELIBERER");
        assertThat(ConseilClasseService.suggestion(bd("8.00"))).isEqualTo("A_DELIBERER");
        assertThat(ConseilClasseService.suggestion(bd("7.99"))).isEqualTo("REDOUBLE");
        assertThat(ConseilClasseService.suggestion(null)).isNull();
    }

    @Test
    void mentions() {
        assertThat(ConseilClasseService.mention(bd("16"))).isEqualTo("FELICITATIONS");
        assertThat(ConseilClasseService.mention(bd("14.5"))).isEqualTo("ENCOURAGEMENTS");
        assertThat(ConseilClasseService.mention(bd("12"))).isEqualTo("TABLEAU_HONNEUR");
        assertThat(ConseilClasseService.mention(bd("11.99"))).isNull();
        assertThat(ConseilClasseService.mention(null)).isNull();
    }

    @Test
    void decisionSuggeree() {
        assertThat(ConseilClasseService.decisionSuggeree("ADMIS")).isEqualTo("PASSAGE");
        assertThat(ConseilClasseService.decisionSuggeree("REDOUBLE")).isEqualTo("REDOUBLEMENT");
        assertThat(ConseilClasseService.decisionSuggeree("A_DELIBERER")).isNull();
        assertThat(ConseilClasseService.decisionSuggeree(null)).isNull();
    }

    @Test
    void observationRequise() {
        assertThat(ConseilClasseService.observationRequise("PASSAGE", "ADMIS")).isFalse();
        assertThat(ConseilClasseService.observationRequise("REDOUBLEMENT", "REDOUBLE")).isFalse();
        assertThat(ConseilClasseService.observationRequise("PASSAGE", "A_DELIBERER")).isFalse();
        assertThat(ConseilClasseService.observationRequise("REDOUBLEMENT", "A_DELIBERER")).isFalse();
        assertThat(ConseilClasseService.observationRequise("PASSAGE", "REDOUBLE")).isTrue();
        assertThat(ConseilClasseService.observationRequise("REDOUBLEMENT", "ADMIS")).isTrue();
        assertThat(ConseilClasseService.observationRequise("EXCLUSION", "REDOUBLE")).isTrue();
        assertThat(ConseilClasseService.observationRequise("PASSAGE", null)).isTrue();
    }

    @Test
    void rangsAvecExAequo() {
        Map<Long, BigDecimal> m = new LinkedHashMap<>();
        m.put(1L, bd("15"));
        m.put(2L, bd("12"));
        m.put(3L, bd("15"));
        m.put(4L, null);
        m.put(5L, bd("9"));
        Map<Long, Integer> r = ConseilClasseService.rangs(m);
        assertThat(r.get(1L)).isEqualTo(1);
        assertThat(r.get(3L)).isEqualTo(1);
        assertThat(r.get(2L)).isEqualTo(3);
        assertThat(r.get(5L)).isEqualTo(4);
        assertThat(r).doesNotContainKey(4L);
    }
}
