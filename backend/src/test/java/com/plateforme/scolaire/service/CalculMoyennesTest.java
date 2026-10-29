package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.plateforme.scolaire.service.CalculMoyennes.NoteCalcul;
import com.plateforme.scolaire.service.CalculMoyennes.Resultat;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class CalculMoyennesTest {

    private static final long MATHS = 1L;
    private static final long LANGUE = 2L;

    private static NoteCalcul note(long inscription, long matiere, String valeur, String bareme, String coefficient) {
        return new NoteCalcul(inscription, matiere, new BigDecimal(valeur), new BigDecimal(bareme),
                new BigDecimal(coefficient));
    }

    private static BigDecimal arrondi(BigDecimal valeur) {
        return CalculMoyennes.arrondir(valeur);
    }

    @Test
    void lesNotesSontRamenneesSurVingtEtPondereesParLeCoefficient() {
        // 14/20 (coef 1) et 8/10 (coef 2) : (14 x 1 + 16 x 2) / 3 = 15,33
        Resultat r = CalculMoyennes.calculer(List.of(
                note(1, MATHS, "14", "20", "1"),
                note(1, MATHS, "8", "10", "2")));
        assertThat(arrondi(r.moyennesParEleve().get(1L).get(MATHS))).isEqualByComparingTo("15.33");
    }

    @Test
    void laMoyenneGeneraleEstLaMoyenneSimpleDesMoyennesDeMatieres() {
        Resultat r = CalculMoyennes.calculer(List.of(
                note(1, MATHS, "14", "20", "1"),
                note(1, MATHS, "8", "10", "2"),   // maths : 15,33
                note(1, LANGUE, "10", "20", "1"))); // langue : 10
        assertThat(arrondi(r.moyennesGenerales().get(1L))).isEqualByComparingTo("12.67");
    }

    @Test
    void lesRangsSontAEgaliteSurLesMoyennesArrondies() {
        Resultat r = CalculMoyennes.calculer(List.of(
                note(1, MATHS, "15", "20", "1"),
                note(2, MATHS, "15", "20", "1"),
                note(3, MATHS, "12", "20", "1")));
        assertThat(r.rangs().get(1L)).isEqualTo(1);
        assertThat(r.rangs().get(2L)).isEqualTo(1);
        assertThat(r.rangs().get(3L)).isEqualTo(3);
    }

    @Test
    void laMoyenneDeClasseParMatiereEstLaMoyenneDesMoyennesDesEleves() {
        Resultat r = CalculMoyennes.calculer(List.of(
                note(1, MATHS, "10", "20", "1"),
                note(2, MATHS, "14", "20", "1")));
        assertThat(arrondi(r.moyennesClasseParMatiere().get(MATHS))).isEqualByComparingTo("12.00");
        assertThat(arrondi(r.moyenneGeneraleClasse())).isEqualByComparingTo("12.00");
    }

    @Test
    void unEleveSansNoteNApparaitPasDansLesResultats() {
        Resultat r = CalculMoyennes.calculer(List.of(note(1, MATHS, "10", "20", "1")));
        assertThat(r.moyennesGenerales()).containsOnlyKeys(1L);
        assertThat(r.rangs()).containsOnlyKeys(1L);
    }

    @Test
    void sansAucuneNoteIlNYAPasDeMoyenne() {
        Resultat r = CalculMoyennes.calculer(List.of());
        assertThat(r.moyennesGenerales()).isEmpty();
        assertThat(r.moyenneGeneraleClasse()).isNull();
    }

    @Test
    void unBaremeNonStandardEstRamenneSurVingt() {
        Resultat r = CalculMoyennes.calculer(List.of(note(1, MATHS, "45", "100", "1")));
        assertThat(arrondi(r.moyennesGenerales().get(1L))).isEqualByComparingTo("9.00");
    }

    @Test
    void lesCoefficientsDeMatieresPonderentLaMoyenneGenerale() {
        // maths 16 (coef 3), langue 8 (coef par défaut 1) : (48 + 8) / 4 = 14
        Resultat r = CalculMoyennes.calculer(List.of(
                note(1, MATHS, "16", "20", "1"),
                note(1, LANGUE, "8", "20", "1")),
                java.util.Map.of(MATHS, new BigDecimal("3")));
        assertThat(arrondi(r.moyennesGenerales().get(1L))).isEqualByComparingTo("14.00");
        assertThat(arrondi(r.moyennesParEleve().get(1L).get(MATHS))).isEqualByComparingTo("16.00");
    }

    @Test
    void unCoefficientNeCompteQueSiLEleveAUneNoteDansLaMatiere() {
        // l'élève 2 n'a pas de note en maths : sa moyenne générale n'est pas tirée vers 0
        Resultat r = CalculMoyennes.calculer(List.of(
                note(1, MATHS, "16", "20", "1"),
                note(2, LANGUE, "10", "20", "1")),
                java.util.Map.of(MATHS, new BigDecimal("3")));
        assertThat(arrondi(r.moyennesGenerales().get(2L))).isEqualByComparingTo("10.00");
    }
}
