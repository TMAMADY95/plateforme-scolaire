package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DeroulementEpreuvesServiceTest {

    @Test
    void lePliSuitLeCircuitJusquAuPresident() {
        assertThat(DeroulementEpreuvesService.etapeSuivante("A_CONDITIONNER")).isEqualTo("CONDITIONNE");
        assertThat(DeroulementEpreuvesService.etapeSuivante("CONDITIONNE")).isEqualTo("EXPEDIE_IRE");
        assertThat(DeroulementEpreuvesService.etapeSuivante("EXPEDIE_IRE")).isEqualTo("RECU_DPE");
        assertThat(DeroulementEpreuvesService.etapeSuivante("RECU_DPE")).isEqualTo("RECU_PRESIDENT");
    }

    @Test
    void ApresLeDernierRelaisLePliNAvancePlusQuePourEtreOuvert() {
        assertThat(DeroulementEpreuvesService.etapeSuivante("RECU_PRESIDENT")).isNull();
        assertThat(DeroulementEpreuvesService.etapeSuivante("OUVERT")).isNull();
    }

    @Test
    void lEcartDeCopiesCompareLesCopiesAuxCandidatsAyantCompose() {
        assertThat(DeroulementEpreuvesService.ecartCopies(28, 1, 29)).isZero();
        assertThat(DeroulementEpreuvesService.ecartCopies(28, 1, 27)).isEqualTo(-2);
        assertThat(DeroulementEpreuvesService.ecartCopies(28, null, 28)).isZero();
        assertThat(DeroulementEpreuvesService.ecartCopies(null, null, null)).isNull();
    }

    @org.junit.jupiter.api.Test
    void sujetTireSeulementSiUnSujetEstTire() {
        assertThat(DeroulementEpreuvesService.sujetTire(java.util.List.of("VALIDE", "TIRE", "VALIDE"))).isTrue();
        assertThat(DeroulementEpreuvesService.sujetTire(java.util.List.of("VALIDE", "COMPROMIS"))).isFalse();
        assertThat(DeroulementEpreuvesService.sujetTire(java.util.List.of())).isFalse();
    }

    @org.junit.jupiter.api.Test
    void pliPerimeSiLeSujetEstRemplaceApresLeConditionnement() {
        var t = java.time.Instant.parse("2026-06-01T10:00:00Z");
        assertThat(DeroulementEpreuvesService.pliPerime(t, t.plusSeconds(60))).isTrue();
        assertThat(DeroulementEpreuvesService.pliPerime(t, t.minusSeconds(60))).isFalse();
        assertThat(DeroulementEpreuvesService.pliPerime(null, t)).isFalse();
        assertThat(DeroulementEpreuvesService.pliPerime(t, null)).isFalse();
    }
}
