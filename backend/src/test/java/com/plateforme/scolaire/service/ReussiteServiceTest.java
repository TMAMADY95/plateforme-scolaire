package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.plateforme.scolaire.domain.Classe;
import com.plateforme.scolaire.domain.Niveau;
import com.plateforme.scolaire.repository.ClasseRepository;
import com.plateforme.scolaire.web.dto.LigneResultatDto;
import com.plateforme.scolaire.web.dto.ResultatsClasseDto;
import com.plateforme.scolaire.web.dto.ReussiteDto;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ReussiteServiceTest {

    private final ClasseRepository classes = mock(ClasseRepository.class);
    private final MoyennesService moyennes = mock(MoyennesService.class);
    private final ReussiteService service = new ReussiteService(classes, moyennes);

    private static Classe classe(long id, String nom) {
        Niveau niveau = new Niveau();
        niveau.setLibelle("6e");
        Classe c = new Classe();
        c.setId(id);
        c.setNom(nom);
        c.setNiveau(niveau);
        return c;
    }

    private static LigneResultatDto ligne(long id, String moyenne) {
        return new LigneResultatDto(id, null, Map.of(), moyenne == null ? null : new BigDecimal(moyenne), null);
    }

    @Test
    void leTauxCompteLesMoyennesAuDessusDuSeuilEtIgnoreLesElevesSansNote() {
        when(classes.findByEtablissementIdAndAnneeScolaireIdOrderByNom(7L, 2L))
                .thenReturn(List.of(classe(1L, "A"), classe(2L, "B")));
        when(moyennes.resultatsClasse(1L, 5L)).thenReturn(new ResultatsClasseDto(1L, 5L, List.of(),
                List.of(ligne(1, "12.50"), ligne(2, "10.00"), ligne(3, "8.00"), ligne(4, null)), Map.of(),
                new BigDecimal("10.17")));
        when(moyennes.resultatsClasse(2L, 5L)).thenReturn(new ResultatsClasseDto(2L, 5L, List.of(),
                List.of(ligne(5, "9.99")), Map.of(), new BigDecimal("9.99")));

        ReussiteDto dto = service.reussite(7L, 2L, 5L);

        assertThat(dto.classes().get(0).evalues()).isEqualTo(3);
        assertThat(dto.classes().get(0).admis()).isEqualTo(2);
        assertThat(dto.classes().get(0).taux()).isEqualByComparingTo("66.7");
        assertThat(dto.classes().get(1).taux()).isEqualByComparingTo("0");
        assertThat(dto.evalues()).isEqualTo(4);
        assertThat(dto.admis()).isEqualTo(2);
        assertThat(dto.taux()).isEqualByComparingTo("50.0");
    }

    @Test
    void sansAucuneNoteLeTauxEstNull() {
        when(classes.findByEtablissementIdAndAnneeScolaireIdOrderByNom(7L, 2L)).thenReturn(List.of(classe(1L, "A")));
        when(moyennes.resultatsClasse(1L, 5L)).thenReturn(new ResultatsClasseDto(1L, 5L, List.of(),
                List.of(ligne(1, null)), Map.of(), null));

        ReussiteDto dto = service.reussite(7L, 2L, 5L);

        assertThat(dto.taux()).isNull();
        assertThat(dto.classes().get(0).taux()).isNull();
    }
}
