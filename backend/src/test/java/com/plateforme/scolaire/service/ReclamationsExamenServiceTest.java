package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.plateforme.scolaire.domain.ReclamationExamen;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReclamationsExamenServiceTest {

    private static final Instant T0 = Instant.parse("2026-07-01T10:00:00Z");

    private static ReclamationExamen r(String statut, Instant depot, Instant traitee) {
        var x = new ReclamationExamen();
        x.setStatut(statut);
        x.setDeposeeLe(depot);
        x.setEcheanceLe(ReclamationsExamenService.echeance(depot));
        x.setTraiteeLe(traitee);
        return x;
    }

    @Test
    void echeanceEtRetard() {
        Instant e = ReclamationsExamenService.echeance(T0);
        assertThat(e).isEqualTo(Instant.parse("2026-07-16T10:00:00Z"));
        assertThat(ReclamationsExamenService.enRetard("OUVERTE", e, e.plusSeconds(1))).isTrue();
        assertThat(ReclamationsExamenService.enRetard("OUVERTE", e, e.minusSeconds(1))).isFalse();
        assertThat(ReclamationsExamenService.enRetard("ACCEPTEE", e, e.plusSeconds(1))).isFalse();
    }

    @Test
    void agregatDesReclamations() {
        Instant maintenant = T0.plusSeconds(20L * 86400);
        var a = QualiteDonneesExamensService.agreger(List.of(
                r("ACCEPTEE", T0, T0.plusSeconds(3 * 86400)),
                r("REJETEE", T0, T0.plusSeconds(18L * 86400)),
                r("OUVERTE", T0, null),
                r("OUVERTE", T0.plusSeconds(15L * 86400), null)), maintenant);
        assertThat(a.total()).isEqualTo(4);
        assertThat(a.ouvertes()).isEqualTo(2);
        assertThat(a.acceptees()).isEqualTo(1);
        assertThat(a.rejetees()).isEqualTo(1);
        assertThat(a.enRetard()).isEqualTo(1);
        assertThat(a.tauxTraiteesDansLeDelai()).isEqualTo(50.0);
        assertThat(a.tauxAcceptation()).isEqualTo(50.0);
    }

    @Test
    void sansReclamationLesTauxSontVides() {
        var a = QualiteDonneesExamensService.agreger(List.of(), T0);
        assertThat(a.total()).isZero();
        assertThat(a.tauxTraiteesDansLeDelai()).isNull();
    }
}
