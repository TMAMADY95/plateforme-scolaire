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
}
