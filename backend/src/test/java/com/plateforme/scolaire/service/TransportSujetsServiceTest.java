package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.plateforme.scolaire.web.dto.ConvoiExamenDto.Convoi;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class TransportSujetsServiceTest {

    private static final Instant PREVU = Instant.parse("2026-06-09T06:00:00Z");

    private static Convoi c(Long centre, String vehicule, String statut, boolean retard, String incident, Integer carburant) {
        return new Convoi(1L, centre, "C" + centre, vehicule, null, null, false, null, PREVU, null, null, carburant, statut, incident, null, null, retard);
    }

    @Test
    void retardDUnConvoi() {
        assertThat(TransportExamenService.enRetard("LIVRE", PREVU, PREVU.plusSeconds(60), PREVU.plusSeconds(9999))).isTrue();
        assertThat(TransportExamenService.enRetard("LIVRE", PREVU, PREVU.minusSeconds(60), PREVU.plusSeconds(9999))).isFalse();
        assertThat(TransportExamenService.enRetard("EN_ROUTE", PREVU, null, PREVU.plusSeconds(1))).isTrue();
        assertThat(TransportExamenService.enRetard("PREVU", PREVU, null, PREVU.minusSeconds(1))).isFalse();
        assertThat(TransportExamenService.enRetard("PREVU", null, null, PREVU)).isFalse();
    }

    @Test
    void resumeDuTransport() {
        var r = TransportExamenService.resume(4, List.of(
                c(1L, "Camion A", "LIVRE", false, null, 40),
                c(2L, "camion a", "LIVRE", true, "RETARD", 30),
                c(3L, "Pick-up B", "EN_ROUTE", true, null, null)));
        assertThat(r.centresDesservis()).isEqualTo(3);
        assertThat(r.centresSansConvoi()).isEqualTo(1);
        assertThat(r.livres()).isEqualTo(2);
        assertThat(r.enRoute()).isEqualTo(1);
        assertThat(r.enRetard()).isEqualTo(2);
        assertThat(r.avecIncident()).isEqualTo(1);
        assertThat(r.vehicules()).isEqualTo(2);
        assertThat(r.carburantLitres()).isEqualTo(70);
        assertThat(r.tauxLivresATemps()).isEqualTo(50.0);
    }

    @Test
    void sujetsAImprimerAvecMarge() {
        assertThat(SujetsAImprimerService.sujets(100)).isEqualTo(105);
        assertThat(SujetsAImprimerService.sujets(30)).isEqualTo(32);
        assertThat(SujetsAImprimerService.sujets(0)).isZero();
        assertThat(SujetsAImprimerService.ecart(90, 105)).isEqualTo(-15);
    }
}
