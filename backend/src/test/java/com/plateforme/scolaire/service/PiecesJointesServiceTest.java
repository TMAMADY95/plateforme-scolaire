package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PiecesJointesServiceTest {

    @Test
    void leTypeVientDuContenuEtPasDuNomDuFichier() {
        assertThat(PiecesJointesService.typeReel("%PDF-1.7 ...".getBytes())).isEqualTo("application/pdf");
        assertThat(PiecesJointesService.typeReel(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00})).isEqualTo("image/jpeg");
        assertThat(PiecesJointesService.typeReel(new byte[] {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10})).isEqualTo("image/png");
        assertThat(PiecesJointesService.typeReel("<script>alert(1)</script>".getBytes())).isNull();
    }

    @Test
    void leNomDuFichierEstAssaini() {
        assertThat(PiecesJointesService.nomSur("..\\..\\etc/passwd")).isEqualTo("passwd");
        assertThat(PiecesJointesService.nomSur("rapport<1>.pdf")).isEqualTo("rapport_1_.pdf");
        assertThat(PiecesJointesService.nomSur(null)).isEqualTo("piece-jointe");
    }
}
