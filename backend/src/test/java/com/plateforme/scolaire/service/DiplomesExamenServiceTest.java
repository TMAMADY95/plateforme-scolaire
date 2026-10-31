package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.Test;

class DiplomesExamenServiceTest {

    @Test
    void laMentionSeDeduitDeLaMoyenne() {
        assertThat(DiplomesExamenService.mention(new BigDecimal("16.00"))).isEqualTo("TRES_BIEN");
        assertThat(DiplomesExamenService.mention(new BigDecimal("15.99"))).isEqualTo("BIEN");
        assertThat(DiplomesExamenService.mention(new BigDecimal("14.00"))).isEqualTo("BIEN");
        assertThat(DiplomesExamenService.mention(new BigDecimal("12.00"))).isEqualTo("ASSEZ_BIEN");
        assertThat(DiplomesExamenService.mention(new BigDecimal("10.00"))).isEqualTo("PASSABLE");
        assertThat(DiplomesExamenService.mention(null)).isEqualTo("PASSABLE");
    }

    @Test
    void lesCodesOntLaBonneLongueurEtSeRecopientSansAmbiguite() {
        var hasard = RandomGenerator.of("L64X128MixRandom");
        var vus = new HashSet<String>();
        for (int i = 0; i < 500; i++) {
            String code = DiplomesExamenService.genererCode(hasard);
            assertThat(code).hasSize(DiplomesExamenService.LONGUEUR_CODE).matches("[A-HJKMNP-Z2-9]+");
            vus.add(code);
        }
        assertThat(vus).hasSize(500);
    }

    @Test
    void laSaisieDUnCodeEstNormalisee() {
        assertThat(DiplomesExamenService.normaliser(" abcde-fghjk ")).isEqualTo("ABCDEFGHJK");
        assertThat(DiplomesExamenService.normaliser(null)).isEmpty();
    }

    @Test
    void leNumeroDeDiplomeEstDeterministe() {
        assertThat(DiplomesExamenService.numero(7L, 42L)).isEqualTo("DIP-7-000042");
    }

    @Test
    void leCodeSAfficheParGroupesDeCinq() {
        assertThat(GenerateurDiplomesPdf.codeLisible("ABCDEFGHJK")).isEqualTo("ABCDE-FGHJK");
    }
}
