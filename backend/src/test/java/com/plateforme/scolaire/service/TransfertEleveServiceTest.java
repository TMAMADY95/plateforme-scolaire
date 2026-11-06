package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TransfertEleveServiceTest {

    @Test
    void seuleUneDemandeOuvertePeutEtreTraitee() {
        assertThat(TransfertEleveService.ouverte("DEMANDE")).isTrue();
        assertThat(TransfertEleveService.ouverte("APPROUVE")).isFalse();
        assertThat(TransfertEleveService.ouverte("REFUSE")).isFalse();
        assertThat(TransfertEleveService.ouverte("ANNULE")).isFalse();
    }

    @Test
    void decisionsValides() {
        assertThat(TransfertEleveService.verifierDecision("APPROUVE", null)).isNull();
        assertThat(TransfertEleveService.verifierDecision("REFUSE", "Frais impayés")).isNull();
    }

    @Test
    void refusSansMotifEtDecisionInconnue() {
        assertThat(TransfertEleveService.verifierDecision("REFUSE", "  ")).isEqualTo("Le motif du refus est obligatoire");
        assertThat(TransfertEleveService.verifierDecision("REFUSE", null)).isNotNull();
        assertThat(TransfertEleveService.verifierDecision("PEUT_ETRE", null)).startsWith("Décision inconnue");
    }

    @Test
    void motifsConnus() {
        assertThat(TransfertEleveService.MOTIFS).contains("DEMENAGEMENT", "DISCIPLINE").doesNotContain("VACANCES");
    }
}
