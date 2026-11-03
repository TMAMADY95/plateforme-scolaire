package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.plateforme.scolaire.web.dto.ConcordanceRapportsDto.Ecart;
import java.util.List;
import org.junit.jupiter.api.Test;

class ConcordanceRapportsServiceTest {

    private static ConcordanceRapportsService.Rapport rapport(Integer p, Integer a, Integer e, Integer copies, boolean clos) {
        return new ConcordanceRapportsService.Rapport(1L, "Maths", p, a, e, copies, clos);
    }

    private static List<String> codes(List<Ecart> l) {
        return l.stream().map(Ecart::code).toList();
    }

    @Test
    void rapportConcordantSansEcart() {
        // 30 candidats : 27 présents, 2 absents, 1 éliminé ; 28 copies (présents + éliminés)
        assertThat(ConcordanceRapportsService.ecarts(30, 27, 1, true, List.of(rapport(27, 2, 1, 28, true)))).isEmpty();
    }

    @Test
    void rapportNonRenseigneIgnore() {
        assertThat(ConcordanceRapportsService.ecarts(30, 0, 0, false, List.of(rapport(null, null, null, null, false)))).isEmpty();
    }

    @Test
    void totalEtCopiesIncoherents() {
        var res = ConcordanceRapportsService.ecarts(30, 0, 0, false, List.of(rapport(25, 2, 1, 30, false)));
        assertThat(codes(res)).containsExactly("TOTAL_INCOHERENT", "COPIES_INCOHERENTES");
        assertThat(res.get(1).gravite()).isEqualTo("AVERTISSEMENT");
        var clos = ConcordanceRapportsService.ecarts(28, 0, 0, false, List.of(rapport(25, 2, 1, 30, true)));
        assertThat(clos).filteredOn(e -> "COPIES_INCOHERENTES".equals(e.code())).extracting(Ecart::gravite).containsExactly("ERREUR");
    }

    @Test
    void presentsEtElimines() {
        // 30 candidats ; 27 déclarés présents et 1 éliminé, mais le rapport en compte 25 présents et 2 éliminés
        var res = ConcordanceRapportsService.ecarts(30, 27, 1, true, List.of(rapport(25, 3, 2, 27, true)));
        assertThat(codes(res)).containsExactlyInAnyOrder("PRESENTS_INFERIEURS", "ELIMINES_NON_REPORTES");
    }

    @Test
    void statutsIncompletsNeDeclenchentPasLesComparaisons() {
        assertThat(ConcordanceRapportsService.ecarts(30, 10, 0, false, List.of(rapport(25, 3, 2, 27, true)))).isEmpty();
    }
}
