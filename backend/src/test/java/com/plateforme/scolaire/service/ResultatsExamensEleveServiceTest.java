package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.plateforme.scolaire.domain.AnneeScolaire;
import com.plateforme.scolaire.domain.CandidatExamen;
import com.plateforme.scolaire.domain.EpreuveExamen;
import com.plateforme.scolaire.domain.ExamenNational;
import com.plateforme.scolaire.repository.CandidatExamenRepository;
import com.plateforme.scolaire.repository.EpreuveExamenRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ResultatsExamensEleveServiceTest {

    @Test
    void visibleSeulementApresProclamation() {
        assertThat(ResultatsExamensEleveService.visible("PROCLAME")).isTrue();
        assertThat(ResultatsExamensEleveService.visible("DELIBERE")).isFalse();
        assertThat(ResultatsExamensEleveService.visible("EN_COURS")).isFalse();
    }

    private static ExamenNational examen(long id, String statut) {
        AnneeScolaire annee = new AnneeScolaire();
        annee.setLibelle("2025-2026");
        ExamenNational e = new ExamenNational();
        ReflectionTestUtils.setField(e, "id", id);
        e.setLibelle("BAC " + id);
        e.setAnneeScolaire(annee);
        e.setStatutResultats(statut);
        return e;
    }

    @Test
    void ignoreLesExamensNonProclames() {
        var candidats = mock(CandidatExamenRepository.class);
        var epreuves = mock(EpreuveExamenRepository.class);
        CandidatExamen proclame = new CandidatExamen();
        proclame.setExamen(examen(1, "PROCLAME"));
        proclame.setStatutEpreuves("PRESENT");
        proclame.setDecision("ADMIS");
        proclame.setMoyenne(new BigDecimal("14.50"));
        EpreuveExamen maths = new EpreuveExamen();
        ReflectionTestUtils.setField(maths, "id", 5L);
        maths.setLibelle("Maths");
        maths.setCoefficient(3);
        proclame.getNotes().put(5L, new BigDecimal("14.50"));
        CandidatExamen enCours = new CandidatExamen();
        enCours.setExamen(examen(2, "DELIBERE"));
        enCours.setStatutEpreuves("PRESENT");
        enCours.setDecision("ADMIS");
        when(candidats.deLEleve(9L)).thenReturn(List.of(proclame, enCours));
        when(epreuves.findByExamenIdOrderByDateEpreuveAscHeureDebutAsc(1L)).thenReturn(List.of(maths));

        var res = new ResultatsExamensEleveService(candidats, epreuves).deLEleve(9L);

        assertThat(res).hasSize(1);
        assertThat(res.get(0).examenId()).isEqualTo(1L);
        assertThat(res.get(0).mention()).isEqualTo("BIEN");
        assertThat(res.get(0).notes()).hasSize(1);
        assertThat(res.get(0).notes().get(0).epreuve()).isEqualTo("Maths");
    }

    @Test
    void absentSansNotesNiMoyenneNiMention() {
        var candidats = mock(CandidatExamenRepository.class);
        var epreuves = mock(EpreuveExamenRepository.class);
        CandidatExamen absent = new CandidatExamen();
        absent.setExamen(examen(1, "PROCLAME"));
        absent.setStatutEpreuves("ABSENT");
        absent.setMoyenne(new BigDecimal("12.00"));
        when(candidats.deLEleve(9L)).thenReturn(List.of(absent));

        var r = new ResultatsExamensEleveService(candidats, epreuves).deLEleve(9L).get(0);

        assertThat(r.notes()).isEmpty();
        assertThat(r.moyenne()).isNull();
        assertThat(r.mention()).isNull();
    }
}
