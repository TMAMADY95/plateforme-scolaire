package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class GenerateurCartesPdfTest {

    private final GenerateurCartesPdf generateur = new GenerateurCartesPdf();

    private static GenerateurCartesPdf.Carte carte(String nom) {
        return new GenerateurCartesPdf.Carte(nom, "M-1", "02/03/2012", "6e A", "C-001", "PV-0001", "Collège Test", "1", "7");
    }

    private static boolean estUnPdf(byte[] octets) {
        return octets.length > 4 && new String(octets, 0, 5, StandardCharsets.ISO_8859_1).startsWith("%PDF");
    }

    @Test
    void desCartesDonnentUnPdfValide_memeEnNombreImpairOuSansDonnees() {
        var impair = new GenerateurCartesPdf.DonneesCartes("Collège Test", "CEE", "2026-2027", "01/06/2027",
                List.of(carte("Awa Diallo"), carte("Mamadou Bah"), carte("Fanta Camara")));
        var sans = new GenerateurCartesPdf.DonneesCartes("Collège Test", "CEE", "2026-2027", null, List.of());

        assertThat(estUnPdf(generateur.cartes(impair))).isTrue();
        assertThat(estUnPdf(generateur.cartes(sans))).isTrue();
    }

    @Test
    void huitCartesParPage() {
        assertThat(GenerateurCartesPdf.pages(0)).isZero();
        assertThat(GenerateurCartesPdf.pages(8)).isEqualTo(1);
        assertThat(GenerateurCartesPdf.pages(9)).isEqualTo(2);
        assertThat(GenerateurCartesPdf.valeur(null)).isEqualTo("—");
        assertThat(GenerateurCartesPdf.valeur(" ")).isEqualTo("—");
    }

    @Test
    void seuleUneListeValideeDonneDesCartes() {
        assertThat(CartesExamenService.imprimable("VALIDEE")).isTrue();
        assertThat(CartesExamenService.imprimable("VALIDEE_REGION")).isFalse();
        assertThat(CartesExamenService.imprimable("TRANSMISE")).isFalse();
        assertThat(CartesExamenService.imprimable("BROUILLON")).isFalse();
    }
}
