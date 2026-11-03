package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class GenerateurDiplomesPdfTest {

    /** PNG de 1 pixel : suffit pour vérifier que la photo s'intègre sans erreur. */
    static final byte[] PNG_1X1 = java.util.Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==");

    @Test
    void intituleSelonLExamen() {
        assertThat(GenerateurDiplomesPdf.intitule("Baccalauréat 2026")).isEqualTo("Diplôme de Bachelier du second Degré");
        assertThat(GenerateurDiplomesPdf.intitule("BEPC 2026")).isEqualTo("Brevet d'Études du Premier Cycle");
        assertThat(GenerateurDiplomesPdf.intitule("CEE blanc")).isEqualTo("Certificat d'Études Élémentaires");
        assertThat(GenerateurDiplomesPdf.intitule("Concours X")).isEqualTo("Diplôme : Concours X");
    }

    @Test
    void lienDeVerificationEtPdfGenere() {
        var generateur = new GenerateurDiplomesPdf("https://exemple.test/");
        assertThat(generateur.lienVerification("ABCDE12345")).isEqualTo("https://exemple.test/verification-diplome?code=ABCDE12345");
        var x = new GenerateurDiplomesPdf.Diplome("Mariama Camara", "ELV-1", "DIP-1-000001", "ABCDE12345", "BIEN", "14/10/2026",
                "SCIENCES_SOCIALES", "143953", "Centre A", "Lycée B", "05/06/2008", "Conakry", "Père", "Mère", "10/10/2026", PNG_1X1);
        byte[] pdf = generateur.diplomes(new GenerateurDiplomesPdf.DonneesDiplomes("Lycée B", "Baccalauréat", "2026-2027", List.of(x)));
        assertThat(new String(pdf, 0, 5, java.nio.charset.StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
    }

    @Test
    void codeEtSerieLisibles() {
        assertThat(GenerateurDiplomesPdf.codeLisible("ABCDE12345")).isEqualTo("ABCDE-12345");
        assertThat(GenerateurDiplomesPdf.libelleSerie("SCIENCES_EXPERIMENTALES")).isEqualTo("SCIENCES EXPÉRIMENTALES");
        assertThat(GenerateurDiplomesPdf.libelleSerie(null)).isEmpty();
    }
}
