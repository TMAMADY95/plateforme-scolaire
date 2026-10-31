package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.plateforme.scolaire.web.dto.LigneEtablissementStatDto;
import com.plateforme.scolaire.web.dto.RapportPostesDto;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class PostesServiceTest {

    private static LigneEtablissementStatDto stats(long id, long eleves, long enseignants) {
        return new LigneEtablissementStatDto(id, "C" + id, "École " + id, "PUBLIC", List.of(), "ACTIF", 1L, "Kindia",
                eleves, enseignants, 1, 5);
    }

    @Test
    void lesPostesVacantsSontLesPostesPrevusMoinsLesEnseignantsEnPoste() {
        var l = PostesService.ligne(stats(1, 300, 6), 10, 40);

        assertThat(l.vacants()).isEqualTo(4);
        assertThat(l.enSurnombre()).isZero();
    }

    @Test
    void unEtablissementAuDessusDesPostesPrevusEstEnSurnombre() {
        var l = PostesService.ligne(stats(1, 100, 8), 5, 40);

        assertThat(l.vacants()).isZero();
        assertThat(l.enSurnombre()).isEqualTo(3);
    }

    @Test
    void sansPostesRenseignesLeBesoinSeCalculeSurLaNorme() {
        // 410 élèves à 40 par enseignant = 11 enseignants nécessaires ; 6 en poste : il en manque 5.
        var l = PostesService.ligne(stats(1, 410, 6), null, 40);

        assertThat(l.postes()).isNull();
        assertThat(l.vacants()).isNull();
        assertThat(l.besoinEstime()).isEqualTo(5);
        assertThat(l.elevesParEnseignant()).isEqualByComparingTo(new BigDecimal("68.3"));
    }

    @Test
    void sansEnseignantLeRatioEstAbsentEtToutLeBesoinEstManquant() {
        var l = PostesService.ligne(stats(1, 80, 0), null, 40);

        assertThat(l.elevesParEnseignant()).isNull();
        assertThat(l.besoinEstime()).isEqualTo(2);
    }

    @Test
    void lesTotauxIgnorentLesPostesNonRenseignes() {
        var t = PostesService.totaux(List.of(
                PostesService.ligne(stats(1, 300, 6), 10, 40),
                PostesService.ligne(stats(2, 200, 2), null, 40)));

        assertThat(t.etablissements()).isEqualTo(2);
        assertThat(t.postes()).isEqualTo(10);
        assertThat(t.vacants()).isEqualTo(4);
        assertThat(t.enseignants()).isEqualTo(8);
        assertThat(t.besoinEstime()).isEqualTo(2 + 3);
    }
}
