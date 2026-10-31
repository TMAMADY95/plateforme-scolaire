package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.plateforme.scolaire.service.GenerateurPdf.DonneesRapport;
import com.plateforme.scolaire.service.GenerateurPdf.SectionRapport;
import java.util.List;
import org.junit.jupiter.api.Test;

class RapportPdfTest {

    @Test
    void leRapportEstUnPdfMemeSansLigne() {
        byte[] pdf = new GenerateurPdf().rapport(new DonneesRapport("Ministère de l'Éducation", "République de Guinée",
                "Rapport sur le réseau scolaire", "Tout le pays", "2026-2027", "30 octobre 2026",
                List.<String[]>of(new String[] {"Élèves inscrits", "12 345"}),
                List.of(new SectionRapport("Effectifs par zone", List.of("Zone", "Élèves"), List.of()),
                        new SectionRapport("Par cycle", List.of("Cycle", "Élèves"),
                                List.of(List.of("Primaire", "1 200"), List.of("Collège", "800"))))));

        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
        assertThat(pdf.length).isGreaterThan(1000);
    }

    @Test
    void lesNombresSontSepareParDesEspaces() {
        assertThat(GenerateurPdf.nombre(1234567)).isEqualTo("1 234 567");
        assertThat(GenerateurPdf.nombre(0)).isEqualTo("0");
    }
}
