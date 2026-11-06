package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.plateforme.scolaire.domain.Eleve;
import com.plateforme.scolaire.repository.EleveRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class IdentifiantNationalTest {

    @Test
    void genererCorrespondALaFonctionDeLaBase() {
        // valeurs calculées par la fonction SQL generer_identifiant_national
        assertThat(IdentifiantNational.generer(26, 123)).isEqualTo("GN-26-0000123-08");
        assertThat(IdentifiantNational.generer(26, 1)).isEqualTo("GN-26-0000001-83");
        assertThat(IdentifiantNational.generer(25, 9_999_999)).isEqualTo("GN-25-9999999-89");
    }

    @Test
    void valideAccepteLesBonsIdentifiantsEtRefuseLesFautes() {
        assertThat(IdentifiantNational.valide("GN-26-0000123-08")).isTrue();
        assertThat(IdentifiantNational.valide("GN-26-0000124-08")).isFalse();
        assertThat(IdentifiantNational.valide("GN-26-0000132-08")).isFalse();
        assertThat(IdentifiantNational.valide("GN-26-0000123-09")).isFalse();
        assertThat(IdentifiantNational.valide("ELV-00000050")).isFalse();
        assertThat(IdentifiantNational.valide(null)).isFalse();
    }

    @Test
    void toutChangementDunChiffreEstDetecte() {
        String id = IdentifiantNational.generer(26, 4_567_890);
        for (int i = 6; i < 13; i++) {
            char c = id.charAt(i);
            if (c == '-') {
                continue;
            }
            char autre = c == '9' ? '0' : (char) (c + 1);
            assertThat(IdentifiantNational.valide(id.substring(0, i) + autre + id.substring(i + 1))).isFalse();
        }
    }

    @Test
    void trouverParMatriculeParIdentifiantNationalEtRefusDuneFaute() {
        EleveRepository repo = mock(EleveRepository.class);
        Eleve e = new Eleve();
        when(repo.findByMatricule("ELV-00000050")).thenReturn(Optional.of(e));
        when(repo.findByMatricule("GN-26-0000123-08")).thenReturn(Optional.empty());
        when(repo.findByIdentifiantNational("GN-26-0000123-08")).thenReturn(Optional.of(e));
        assertThat(IdentifiantNational.trouver(repo, " ELV-00000050 ")).containsSame(e);
        assertThat(IdentifiantNational.trouver(repo, "gn-26-0000123-08")).containsSame(e);
        assertThatThrownBy(() -> IdentifiantNational.trouver(repo, "GN-26-0000124-08")).isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("invalide");
    }
}
