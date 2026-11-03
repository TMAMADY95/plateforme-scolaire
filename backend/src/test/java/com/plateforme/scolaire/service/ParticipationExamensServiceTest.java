package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.plateforme.scolaire.web.dto.ParticipationDto.Epreuve;
import java.util.List;
import org.junit.jupiter.api.Test;

class ParticipationExamensServiceTest {

    private static Epreuve e(int attendus, int presents, int absents, int debutsOk, int debutsTot) {
        return new Epreuve(1L, "BAC", "Mathématiques", "2026-06-10", "08:00", "11:00", 4, 4, 3, attendus, presents, absents, 1, presents, null, null,
                debutsOk, debutsTot, debutsOk, debutsTot);
    }

    @Test
    void tauxDePresence() {
        assertThat(ParticipationExamensService.tauxPresence(90, 100)).isEqualTo(90.0);
        assertThat(ParticipationExamensService.tauxPresence(0, 0)).isNull();
    }

    @Test
    void resumeDesEpreuves() {
        var r = ParticipationExamensService.resume(List.of(e(100, 90, 10, 3, 4), e(100, 80, 20, 4, 4)), 8);
        assertThat(r.epreuves()).isEqualTo(2);
        assertThat(r.rapportsAttendus()).isEqualTo(8);
        assertThat(r.rapportsRecus()).isEqualTo(8);
        assertThat(r.attendus()).isEqualTo(200);
        assertThat(r.presents()).isEqualTo(170);
        assertThat(r.tauxPresence()).isEqualTo(85.0);
        assertThat(r.tauxAbsence()).isEqualTo(15.0);
        assertThat(r.tauxDebutsALHeure()).isEqualTo(87.5);
    }

    @Test
    void sansEpreuveLesTauxSontVides() {
        var r = ParticipationExamensService.resume(List.of(), 0);
        assertThat(r.tauxPresence()).isNull();
        assertThat(r.tauxDebutsALHeure()).isNull();
    }
}
