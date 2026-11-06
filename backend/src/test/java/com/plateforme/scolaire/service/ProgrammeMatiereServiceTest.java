package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.plateforme.scolaire.domain.Matiere;
import com.plateforme.scolaire.domain.ProgrammeMatiere;
import com.plateforme.scolaire.domain.TypeEtablissement;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class ProgrammeMatiereServiceTest {

    private static ProgrammeMatiere ligne(long matiereId, String serie, String coef) {
        Matiere m = new Matiere();
        m.setId(matiereId);
        m.setCode("M" + matiereId);
        ProgrammeMatiere p = new ProgrammeMatiere();
        p.setMatiere(m);
        p.setSerie(serie);
        p.setCoefficient(coef == null ? null : new BigDecimal(coef));
        p.setHeuresHebdo(new BigDecimal("2"));
        return p;
    }

    @Test
    void lignePropreALaSerieEmporteSurLaLigneCommune() {
        Map<Long, BigDecimal> r = ProgrammeMatiereService.coefficientsResolus(List.of(
                ligne(1, null, "2"), ligne(1, "SCIENCES_SOCIALES", "4"), ligne(2, null, null), ligne(3, null, "1.5")));
        assertThat(r).containsOnlyKeys(1L, 3L);
        assertThat(r.get(1L)).isEqualByComparingTo("4");
        assertThat(r.get(3L)).isEqualByComparingTo("1.5");
    }

    @Test
    void ordreDesLignesNeChangePasLeResultat() {
        Map<Long, BigDecimal> r = ProgrammeMatiereService.coefficientsResolus(List.of(ligne(1, "SCIENCES_SOCIALES", "4"), ligne(1, null, "2")));
        assertThat(r.get(1L)).isEqualByComparingTo("4");
    }

    @Test
    void totalDesHeures() {
        assertThat(ProgrammeMatiereService.totalHeures(List.of(ligne(1, null, null), ligne(2, null, null)))).isEqualByComparingTo("4");
    }

    @Test
    void lectureDuFichierCoc() throws Exception {
        List<String> lignes;
        try (var l = new BufferedReader(new InputStreamReader(new ClassPathResource(ProgrammeMatiereService.RESSOURCE_COC).getInputStream(), StandardCharsets.UTF_8))) {
            lignes = l.lines().toList();
        }
        var coc = ProgrammeMatiereService.lireCoc(lignes);
        assertThat(coc).isNotEmpty();
        // collège : 32 h par niveau
        BigDecimal college = coc.stream().filter(x -> x.ordres().contains(7)).map(x -> x.heures()).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(college).isEqualByComparingTo("32");
        // 11e littéraire (sciences sociales) : 32 h
        BigDecimal litteraire = coc.stream().filter(x -> x.ordres().contains(11) && "SCIENCES_SOCIALES".equals(x.serie()))
                .map(x -> x.heures()).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(litteraire).isEqualByComparingTo("32");
        // aucune grille inventée pour la 12e et la terminale
        assertThat(coc.stream().anyMatch(x -> x.ordres().contains(12) || x.ordres().contains(13))).isFalse();
    }

    @Test
    void ligneInvalideRefusee() {
        assertThatThrownBy(() -> ProgrammeMatiereService.lireCoc(List.of("1;;X"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ProgrammeMatiereService.lireCoc(List.of("11;LETTRES;X;Y;1;;"))).isInstanceOf(IllegalArgumentException.class);
    }

    private static ProgrammeMatiere ligneType(long matiereId, String serie, Long typeId, String coef) {
        ProgrammeMatiere p = ligne(matiereId, serie, coef);
        if (typeId != null) {
            TypeEtablissement t = new TypeEtablissement();
            t.setId(typeId);
            p.setTypeEtablissement(t);
        }
        return p;
    }

    @Test
    void ligneDuTypeEmporteSurLaLigneCommune() {
        var retenues = ProgrammeApplicable.retenir(List.of(ligneType(1, null, null, "2"), ligneType(1, null, 5L, "3"), ligneType(2, null, null, "1")));
        assertThat(retenues).hasSize(2);
        assertThat(retenues.get(0).getCoefficient()).isEqualByComparingTo("3");
    }

    @Test
    void typeEtSerieLEmportentSurLeTypeSeul() {
        var retenues = ProgrammeApplicable.retenir(List.of(ligneType(1, null, 5L, "3"), ligneType(1, "SCIENCES_SOCIALES", 5L, "4"), ligneType(1, "SCIENCES_SOCIALES", null, "9")));
        assertThat(retenues).hasSize(1);
        assertThat(retenues.get(0).getCoefficient()).isEqualByComparingTo("4");
    }

    @Test
    void unTypePeutAjouterUneMatiere() {
        var retenues = ProgrammeApplicable.retenir(List.of(ligneType(1, null, null, "2"), ligneType(7, null, 5L, "2")));
        assertThat(retenues).extracting(p -> p.getMatiere().getId()).containsExactly(1L, 7L);
    }

    @Test
    void manuelAuProgrammeSeulementSiLaMatiereEstEnseignee() {
        Map<Long, java.util.Set<Long>> programme = Map.of(11L, java.util.Set.of(1L, 2L));
        assertThat(ManuelsService.auProgramme(programme, 11L, 1L)).isTrue();
        assertThat(ManuelsService.auProgramme(programme, 11L, 9L)).isFalse();
        assertThat(ManuelsService.auProgramme(programme, 12L, 9L)).isTrue();
    }
}
