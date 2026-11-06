package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.plateforme.scolaire.web.dto.StatistiquesExamensDto.Resultats;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

class StatistiquesExamensServiceTest {

    @Test
    void tauxDeReussiteArrondiAuDixieme() {
        assertThat(StatistiquesExamensService.taux(80, 100)).isEqualTo(80.0);
        assertThat(StatistiquesExamensService.taux(1, 3)).isEqualTo(33.3);
        assertThat(StatistiquesExamensService.taux(0, 0)).isNull();
    }

    @Test
    void remplissageDesSallesDExamen() {
        assertThat(StatistiquesExamensService.remplissage(60, 2)).isEqualTo(100.0);
        assertThat(StatistiquesExamensService.remplissage(45, 3)).isEqualTo(50.0);
        assertThat(StatistiquesExamensService.remplissage(10, 0)).isNull();
    }

    /** Un candidat affecté à un centre mais pas encore placé en salle n'occupe aucune place (le taux dépassait 100 %). */
    @Test
    void remplissageNeCompteQueLesCandidatsPlacesEnSalle() {
        EntityManager em = mock(EntityManager.class);
        Query vide = mock(Query.class, Answers.RETURNS_SELF);
        when(vide.getResultList()).thenReturn(List.of());
        when(vide.getSingleResult()).thenReturn(0L);
        Query centres = mock(Query.class, Answers.RETURNS_SELF);
        when(centres.getResultList()).thenReturn(List.of(
                // id, nom, zone, examen, candidats, salles utilisées, établissement, candidats placés en salle
                new Object[] {1L, "Centre A", "Kaloum", "BAC", 60L, 2L, null, 45L},
                new Object[] {2L, "Centre B", "Dixinn", "BEPC", 40L, 0L, null, 0L}));
        when(em.createQuery(anyString())).thenReturn(vide);
        when(em.createQuery(contains("from CentreExamen ce"))).thenReturn(centres);

        var stats = new StatistiquesExamensService(em).statistiques(1L);

        assertThat(stats.salles().candidatsAffectes()).isEqualTo(100);
        assertThat(stats.salles().sallesUtilisees()).isEqualTo(2);
        assertThat(stats.salles().remplissage()).isEqualTo(75.0);
        assertThat(stats.centres().get(0).candidats()).isEqualTo(60);
        assertThat(stats.centres().get(0).remplissage()).isEqualTo(75.0);
        assertThat(stats.centres().get(1).remplissage()).isNull();
    }

    @Test
    void cumulDeLignesDeResultats() {
        var total = StatistiquesExamensService.cumuler("BEPC", List.of(
                new Resultats("Public", 2, 120, 100, 60, 15, 5, 60.0),
                new Resultats("Privé", 1, 50, 40, 30, 8, 2, 75.0)));
        assertThat(total.etablissements()).isEqualTo(3);
        assertThat(total.inscrits()).isEqualTo(170);
        assertThat(total.presentes()).isEqualTo(140);
        assertThat(total.admis()).isEqualTo(90);
        assertThat(total.absents()).isEqualTo(23);
        assertThat(total.elimines()).isEqualTo(7);
        assertThat(total.taux()).isEqualTo(64.3);
    }

    @Test
    void seriesDuLyceeAvecDetailParType() {
        var m = new java.util.LinkedHashMap<String, java.util.Map<String, int[]>>();
        m.put("Sciences mathématiques", java.util.Map.of("Public", new int[] {40, 20, 45}, "Privé", new int[] {10, 8, 12}));
        var series = StatistiquesExamensService.seriesDuNiveau(m);
        assertThat(series).hasSize(1);
        assertThat(series.get(0).inscrits()).isEqualTo(57);
        assertThat(series.get(0).presentes()).isEqualTo(50);
        assertThat(series.get(0).admis()).isEqualTo(28);
        assertThat(series.get(0).taux()).isEqualTo(56.0);
        assertThat(series.get(0).parType()).hasSize(2);
        assertThat(StatistiquesExamensService.seriesDuNiveau(null)).isEmpty();
        assertThat(StatistiquesExamensService.libelleSerie("SCIENCES_SOCIALES")).isEqualTo("Sciences sociales");
    }
}
