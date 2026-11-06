package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.plateforme.scolaire.web.dto.AnnuaireDto;
import java.util.List;
import org.junit.jupiter.api.Test;

class AnnuaireStatistiqueServiceTest {

    @Test
    void ratioUneDecimaleOuNul() {
        assertThat(AnnuaireStatistiqueService.ratio(100, 3)).isEqualTo(33.3);
        assertThat(AnnuaireStatistiqueService.ratio(5, 0)).isNull();
    }

    @Test
    void champCsvProtegeSeparateurEtGuillemets() {
        assertThat(AnnuaireStatistiqueService.champ("Collège; Conakry")).isEqualTo("\"Collège; Conakry\"");
        assertThat(AnnuaireStatistiqueService.champ("dit \"x\"")).isEqualTo("\"dit \"\"x\"\"\"");
        assertThat(AnnuaireStatistiqueService.champ(null)).isEmpty();
    }

    @Test
    void csvContientLesRubriquesEtLaVirguleDecimale() {
        AnnuaireDto a = new AnnuaireDto(1L, "2026-2027", new AnnuaireDto.Generalites(2, 3, 100, 48, 5, 33.3, 25.0),
                List.of(new AnnuaireDto.Niveau(1L, "7e année", 3, 100, 48, 5, 33.3)), List.of(new AnnuaireDto.Age(13, 20, 18)),
                List.of(new AnnuaireDto.Type("Classique", 2, 100)), new AnnuaireDto.Enseignants(4, 1, 3, 2, 1));
        String csv = AnnuaireStatistiqueService.csv(a);
        assertThat(csv).startsWith("﻿").contains("7e année;3;100;48;5;33,3").contains("13;20;18").contains("Classique;2;100").contains(";25,0");
    }
}
