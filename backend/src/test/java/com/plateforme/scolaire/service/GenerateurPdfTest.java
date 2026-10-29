package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class GenerateurPdfTest {

    private final GenerateurPdf generateur = new GenerateurPdf();

    private static boolean estUnPdf(byte[] contenu) {
        return contenu.length > 500 && new String(contenu, 0, 5, StandardCharsets.ISO_8859_1).equals("%PDF-");
    }

    @Test
    void unBulletinEstUnPdfValide_memeSansNotesNiRang() {
        var complet = new GenerateurPdf.DonneesBulletin("Collège Test", "2026-2027", "6e A", "Trimestre 1",
                "Awa Diallo", "M-7",
                List.of(new GenerateurPdf.LigneBulletin("Mathématiques", new BigDecimal("14.00"), new BigDecimal("12.50"))),
                new BigDecimal("14.00"), new BigDecimal("12.50"), 1, 30);
        var vide = new GenerateurPdf.DonneesBulletin("Collège Test", "2026-2027", "6e A", "Trimestre 1",
                "Awa Diallo", "M-7", List.of(), null, null, null, 0);

        assertThat(estUnPdf(generateur.bulletin(complet))).isTrue();
        assertThat(estUnPdf(generateur.bulletin(vide))).isTrue();
    }

    @Test
    void unRecuEstUnPdfValide() {
        var donnees = new GenerateurPdf.DonneesRecu("Collège Test", "FAC-2026-000001", "Scolarité", "Awa Diallo", "M-7",
                "XOF", new BigDecimal("100000"), new BigDecimal("20000"),
                List.of(new GenerateurPdf.LignePaiement("28/10/2026", "Espèces", "PAY-ABC", new BigDecimal("50000"))),
                new BigDecimal("50000"), new BigDecimal("30000"), "29/10/2026");

        assertThat(estUnPdf(generateur.recu(donnees))).isTrue();
    }

    @Test
    void lesFormatsSuiventLaDeviseEtLeFrancais() {
        assertThat(GenerateurPdf.montant(new BigDecimal("12500"), "XOF")).isEqualTo("12 500 XOF");
        assertThat(GenerateurPdf.montant(new BigDecimal("12.5"), "EUR")).isEqualTo("12,50 €");
        assertThat(GenerateurPdf.note(null)).isEqualTo("—");
        assertThat(GenerateurPdf.note(new BigDecimal("15.333"))).isEqualTo("15,33");
        assertThat(GenerateurPdf.ecart(new BigDecimal("14"), new BigDecimal("12.8"))).isEqualTo("+1,20");
        assertThat(GenerateurPdf.ecart(new BigDecimal("12"), new BigDecimal("12.5"))).isEqualTo("-0,50");
        assertThat(GenerateurPdf.rang(1)).isEqualTo("1er");
        assertThat(GenerateurPdf.rang(4)).isEqualTo("4e");
    }

    @Test
    void leCoefficientEstAfficheSansZerosInutiles() {
        assertThat(GenerateurPdf.coefficient(new BigDecimal("3.00"))).isEqualTo("3");
        assertThat(GenerateurPdf.coefficient(new BigDecimal("2.50"))).isEqualTo("2,5");
        assertThat(GenerateurPdf.coefficient(null)).isEqualTo("1");
    }
}
