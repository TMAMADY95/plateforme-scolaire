package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.plateforme.scolaire.domain.Etablissement;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class MutationPublicTest {

    private static Etablissement ecole(String secteur) {
        Etablissement e = new Etablissement();
        e.setSecteur(secteur);
        return e;
    }

    @Test
    void uneMutationSeFaitEntreDeuxEtablissementsPublics() {
        assertThatCode(() -> PostesService.verifierPublics(ecole("PUBLIC"), ecole("PUBLIC"))).doesNotThrowAnyException();
    }

    @Test
    void unEtablissementPriveEnOrigineOuEnAccueilEstRefuse() {
        assertThatThrownBy(() -> PostesService.verifierPublics(ecole("PRIVE"), ecole("PUBLIC")))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> PostesService.verifierPublics(ecole("PUBLIC"), ecole("PRIVE")))
                .isInstanceOf(ResponseStatusException.class);
    }
}
