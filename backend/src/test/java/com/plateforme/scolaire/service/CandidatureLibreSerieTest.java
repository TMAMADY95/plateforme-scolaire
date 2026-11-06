package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.plateforme.scolaire.domain.CycleScolaire;
import com.plateforme.scolaire.domain.ExamenNational;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class CandidatureLibreSerieTest {

    private static ExamenNational examen(String cycle) {
        CycleScolaire c = new CycleScolaire();
        c.setCode(cycle);
        ExamenNational e = new ExamenNational();
        e.setCycle(c);
        return e;
    }

    @Test
    void auLyceeLaSerieEstObligatoire() {
        assertThat(CandidatureLibreService.serieLibre(examen("LYCEE"), "SCIENCES_MATHEMATIQUES")).isEqualTo("SCIENCES_MATHEMATIQUES");
        assertThatThrownBy(() -> CandidatureLibreService.serieLibre(examen("LYCEE"), null)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> CandidatureLibreService.serieLibre(examen("LYCEE"), "INCONNUE")).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void horsLyceeAucuneSerie() {
        assertThat(CandidatureLibreService.serieLibre(examen("COLLEGE"), null)).isNull();
        assertThatThrownBy(() -> CandidatureLibreService.serieLibre(examen("COLLEGE"), "SCIENCES_SOCIALES")).isInstanceOf(ResponseStatusException.class);
    }
}
