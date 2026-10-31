package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.plateforme.scolaire.domain.ExamenNational;
import com.plateforme.scolaire.domain.TypeEtablissement;
import org.junit.jupiter.api.Test;

class ExamenTypesParticipantsTest {

    private static TypeEtablissement type(long id) {
        TypeEtablissement t = new TypeEtablissement();
        t.setId(id);
        return t;
    }

    @Test
    void sansRestrictionTousLesTypesParticipent() {
        ExamenNational examen = new ExamenNational();

        assertThat(examen.accepteType(type(1))).isTrue();
        assertThat(examen.accepteType(null)).isTrue();
    }

    @Test
    void avecRestrictionSeulsLesTypesChoisisParticipent() {
        ExamenNational examen = new ExamenNational();
        examen.getTypesEtablissement().add(type(2));

        assertThat(examen.accepteType(type(2))).isTrue();
        assertThat(examen.accepteType(type(1))).isFalse();
        assertThat(examen.accepteType(null)).isFalse();
    }
}
