package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PhotosEleveServiceTest {

    @Test
    void seulesLesPhotosJpegEtPngSontAcceptees() {
        assertThat(PhotosEleveService.typePhoto(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00})).isEqualTo("image/jpeg");
        assertThat(PhotosEleveService.typePhoto(new byte[] {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A})).isEqualTo("image/png");
        assertThat(PhotosEleveService.typePhoto("%PDF-1.4".getBytes())).isNull();
        assertThat(PhotosEleveService.typePhoto("texte".getBytes())).isNull();
    }
}
