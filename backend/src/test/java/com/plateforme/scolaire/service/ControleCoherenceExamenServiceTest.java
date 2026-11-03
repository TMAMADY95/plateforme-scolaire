package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.plateforme.scolaire.domain.CandidatExamen;
import com.plateforme.scolaire.domain.CentreExamen;
import com.plateforme.scolaire.domain.Eleve;
import com.plateforme.scolaire.domain.EpreuveExamen;
import com.plateforme.scolaire.web.dto.AnomalieExamenDto;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ControleCoherenceExamenServiceTest {

    private static EpreuveExamen epreuve(long id, String libelle, int coef) {
        EpreuveExamen e = new EpreuveExamen();
        ReflectionTestUtils.setField(e, "id", id);
        e.setLibelle(libelle);
        e.setCoefficient(coef);
        return e;
    }

    private static CandidatExamen candidat(long id, long eleveId, String pv, String statut) {
        Eleve el = new Eleve();
        ReflectionTestUtils.setField(el, "id", eleveId);
        el.setNom("N" + eleveId);
        el.setPrenom("P");
        CandidatExamen c = new CandidatExamen();
        ReflectionTestUtils.setField(c, "id", id);
        c.setEleve(el);
        c.setNumeroPv(pv);
        c.setStatutEpreuves(statut);
        return c;
    }

    private static List<String> codes(List<AnomalieExamenDto> a) {
        return a.stream().map(AnomalieExamenDto::code).toList();
    }

    @Test
    void pvManquantEtDoublon() {
        var eps = List.of(epreuve(1, "Maths", 2));
        var a = candidat(1, 1, null, "INSCRIT");
        var b = candidat(2, 2, "PV1", "INSCRIT");
        var c = candidat(3, 3, "PV1", "INSCRIT");
        var codes = codes(ControleCoherenceExamenService.detecter(List.of(a, b, c), eps));
        assertThat(codes).contains("PV_MANQUANT").filteredOn("PV_DOUBLON"::equals).hasSize(2);
    }

    @Test
    void candidatSansAnomalieDeNotes() {
        var eps = List.of(epreuve(1, "Maths", 2), epreuve(2, "Français", 1));
        var a = candidat(1, 1, "PV1", "PRESENT");
        a.getNotes().put(1L, new BigDecimal("12.00"));
        a.getNotes().put(2L, new BigDecimal("9.00"));
        a.setMoyenne(new BigDecimal("11.00"));
        a.setDecision("ADMIS");
        var codes = codes(ControleCoherenceExamenService.detecter(List.of(a), eps));
        assertThat(codes).doesNotContain("NOTES_MANQUANTES", "MOYENNE_INCOHERENTE", "DECISION_INCOHERENTE", "PV_MANQUANT");
    }

    @Test
    void notesManquantesEtMoyenneIncoherente() {
        var eps = List.of(epreuve(1, "Maths", 1), epreuve(2, "Français", 1));
        var manque = candidat(1, 1, "PV1", "PRESENT");
        manque.getNotes().put(1L, new BigDecimal("10.00"));
        var faux = candidat(2, 2, "PV2", "PRESENT");
        faux.getNotes().put(1L, new BigDecimal("10.00"));
        faux.getNotes().put(2L, new BigDecimal("12.00"));
        faux.setMoyenne(new BigDecimal("15.00"));
        faux.setDecision("ADMIS");
        var res = ControleCoherenceExamenService.detecter(List.of(manque, faux), eps);
        assertThat(codes(res)).contains("NOTES_MANQUANTES", "MOYENNE_INCOHERENTE");
    }

    @Test
    void decisionIncoherenteAvecMoyenneJuste() {
        var eps = List.of(epreuve(1, "Maths", 1));
        var c = candidat(1, 1, "PV1", "PRESENT");
        c.getNotes().put(1L, new BigDecimal("8.00"));
        c.setMoyenne(new BigDecimal("8.00"));
        c.setDecision("ADMIS");
        assertThat(codes(ControleCoherenceExamenService.detecter(List.of(c), eps))).contains("DECISION_INCOHERENTE");
    }

    @Test
    void absentOuElimineAvecNotesEtEliminePasNonAdmis() {
        var eps = List.of(epreuve(1, "Maths", 1));
        var absent = candidat(1, 1, "PV1", "ABSENT");
        absent.getNotes().put(1L, new BigDecimal("10.00"));
        var elimine = candidat(2, 2, "PV2", "ELIMINE");
        elimine.setDecision("ADMIS");
        var codes = codes(ControleCoherenceExamenService.detecter(List.of(absent, elimine), eps));
        assertThat(codes).contains("NOTES_SUR_ABSENT", "DECISION_INCOHERENTE");
    }

    @Test
    void notesSuspectes() {
        var eps = List.of(epreuve(1, "Maths", 1));
        var zero = candidat(1, 1, "PV1", "PRESENT");
        zero.getNotes().put(1L, new BigDecimal("0.00"));
        var haute = candidat(2, 2, "PV2", "PRESENT");
        haute.getNotes().put(1L, new BigDecimal("19.50"));
        var horsBareme = candidat(3, 3, "PV3", "PRESENT");
        horsBareme.getNotes().put(1L, new BigDecimal("25.00"));
        var res = ControleCoherenceExamenService.detecter(List.of(zero, haute, horsBareme), eps);
        assertThat(codes(res)).contains("NOTE_ZERO", "NOTE_ELEVEE", "NOTE_HORS_BAREME");
    }

    @Test
    void noteTresEloigneeDeLaMoyenneDeLEpreuve() {
        var eps = List.of(epreuve(1, "Maths", 1));
        var liste = new java.util.ArrayList<CandidatExamen>();
        for (int i = 1; i <= 5; i++) {
            var c = candidat(i, i, "PV" + i, "PRESENT");
            c.getNotes().put(1L, new BigDecimal("4.00"));
            liste.add(c);
        }
        var fort = candidat(6, 6, "PV6", "PRESENT");
        fort.getNotes().put(1L, new BigDecimal("18.00"));
        liste.add(fort);
        var res = ControleCoherenceExamenService.detecter(liste, eps);
        assertThat(res).filteredOn(a -> "NOTE_ECART".equals(a.code())).extracting(AnomalieExamenDto::candidatId).containsExactly(6L);
    }

    @Test
    void affectationsDoublonEtSalleSurchargee() {
        var eps = List.of(epreuve(1, "Maths", 1));
        CentreExamen centre = new CentreExamen();
        ReflectionTestUtils.setField(centre, "id", 7L);
        var liste = new java.util.ArrayList<CandidatExamen>();
        for (int i = 1; i <= 31; i++) {
            var c = candidat(i, i, "PV" + i, "INSCRIT");
            c.setCentre(centre);
            c.setSalle(1);
            c.setTableNumero(i == 2 ? 1 : i);
            liste.add(c);
        }
        var sansCentre = candidat(40, 40, "PV40", "INSCRIT");
        liste.add(sansCentre);
        var codes = codes(ControleCoherenceExamenService.detecter(liste, eps));
        assertThat(codes).contains("PLACE_DOUBLON", "SALLE_SURCHARGEE", "SANS_CENTRE");
    }

    @Test
    void candidatInscritDeuxFois() {
        var eps = List.of(epreuve(1, "Maths", 1));
        var a = candidat(1, 5, "PV1", "INSCRIT");
        var b = candidat(2, 5, "PV2", "INSCRIT");
        assertThat(codes(ControleCoherenceExamenService.detecter(List.of(a, b), eps))).contains("CANDIDAT_DOUBLON");
    }
}
