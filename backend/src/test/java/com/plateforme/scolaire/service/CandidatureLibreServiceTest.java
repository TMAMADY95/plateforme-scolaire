package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CandidatureLibreServiceTest {

    @Test
    void laCandidatureLibreEstOuverteAuBepcEtAuBacSeulement() {
        assertThat(CandidatureLibreService.ouverte("REGION")).isTrue();
        assertThat(CandidatureLibreService.ouverte("NATIONAL")).isTrue();
        assertThat(CandidatureLibreService.ouverte("PREFECTURE")).isFalse();
        assertThat(CandidatureLibreService.ouverte(null)).isFalse();
    }
}
