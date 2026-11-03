package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.plateforme.scolaire.domain.Matiere;
import com.plateforme.scolaire.domain.ProgrammeMatiere;
import com.plateforme.scolaire.web.dto.ConformiteProgrammeDto.Ligne;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ConformiteProgrammeServiceTest {

    private static Matiere matiere(long id, String libelle) {
        Matiere m = new Matiere();
        m.setId(id);
        m.setCode("M" + id);
        m.setLibelle(libelle);
        return m;
    }

    private static ProgrammeMatiere prevu(Matiere m, String heures) {
        ProgrammeMatiere p = new ProgrammeMatiere();
        p.setMatiere(m);
        p.setHeuresHebdo(heures == null ? null : new BigDecimal(heures));
        return p;
    }

    private static String statut(List<Ligne> lignes, String matiere) {
        return lignes.stream().filter(l -> l.matiere().equals(matiere)).findFirst().orElseThrow().statut();
    }

    @Test
    void statutsParMatiere() {
        Matiere maths = matiere(1, "Mathématiques"), fr = matiere(2, "Français"), ang = matiere(3, "Anglais"),
                eps = matiere(4, "EPS"), art = matiere(5, "Arts"), hist = matiere(6, "Histoire");
        Map<Long, ProgrammeMatiere> prevus = new LinkedHashMap<>();
        prevus.put(1L, prevu(maths, "6"));
        prevus.put(2L, prevu(fr, "8"));
        prevus.put(3L, prevu(ang, "2"));
        prevus.put(4L, prevu(eps, "2"));
        prevus.put(6L, prevu(hist, null));
        Map<Long, Matiere> matieres = Map.of(1L, maths, 2L, fr, 3L, ang, 5L, art, 6L, hist);
        Map<Long, Long> minutes = Map.of(1L, 360L, 2L, 420L, 3L, 150L, 5L, 60L, 6L, 60L);

        List<Ligne> lignes = ConformiteProgrammeService.comparer(prevus, matieres, minutes);

        assertThat(statut(lignes, "Mathématiques")).isEqualTo("CONFORME");
        assertThat(statut(lignes, "Français")).isEqualTo("MANQUE");
        assertThat(statut(lignes, "Anglais")).isEqualTo("EXCES");
        assertThat(statut(lignes, "EPS")).isEqualTo("NON_PLANIFIEE");
        assertThat(statut(lignes, "Arts")).isEqualTo("HORS_PROGRAMME");
        assertThat(statut(lignes, "Histoire")).isEqualTo("A_PRECISER");
        assertThat(lignes.stream().filter(l -> l.matiere().equals("Français")).findFirst().orElseThrow().ecart()).isEqualByComparingTo("-1");
    }

    @Test
    void conversionMinutesEnHeures() {
        assertThat(ConformiteProgrammeService.heures(90)).isEqualByComparingTo("1.5");
        assertThat(ConformiteProgrammeService.heures(0)).isEqualByComparingTo("0");
    }
}
