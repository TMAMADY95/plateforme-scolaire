package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.plateforme.scolaire.domain.ProfilEnseignant;
import com.plateforme.scolaire.web.dto.ProfilEnseignantDto;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ProfilEnseignantServiceTest {

    private static ProfilEnseignant profil(String statut, String academique, String professionnelle) {
        ProfilEnseignant p = new ProfilEnseignant();
        p.setStatut(statut);
        p.setFormationAcademique(academique);
        p.setFormationProfessionnelle(professionnelle);
        return p;
    }

    private static ProfilEnseignantDto.Requete requete(LocalDate naissance, LocalDate service) {
        return new ProfilEnseignantDto.Requete(naissance, "F", "FONCTIONNAIRE", "LICENCE", "ENS", null, service, null);
    }

    @Test
    void ageEnAnneesRevolues() {
        assertThat(ProfilEnseignantService.age(LocalDate.of(1990, 11, 6), LocalDate.of(2026, 11, 5))).isEqualTo(35);
        assertThat(ProfilEnseignantService.age(LocalDate.of(1990, 11, 5), LocalDate.of(2026, 11, 5))).isEqualTo(36);
        assertThat(ProfilEnseignantService.age(null, LocalDate.of(2026, 11, 5))).isNull();
    }

    @Test
    void profilRenseigneSiStatutEtFormationAcademiqueConnus() {
        assertThat(ProfilEnseignantService.renseigne(null)).isFalse();
        assertThat(ProfilEnseignantService.renseigne(profil("CONTRACTUEL", null, null))).isFalse();
        assertThat(ProfilEnseignantService.renseigne(profil("CONTRACTUEL", "BAC", null))).isTrue();
    }

    @Test
    void formeSeulementAvecUneFormationProfessionnelle() {
        assertThat(ProfilEnseignantService.forme(null)).isFalse();
        assertThat(ProfilEnseignantService.forme(profil("VOLONTAIRE", "BAC", null))).isFalse();
        assertThat(ProfilEnseignantService.forme(profil("VOLONTAIRE", "BAC", "AUCUNE"))).isFalse();
        assertThat(ProfilEnseignantService.forme(profil("FONCTIONNAIRE", "LICENCE", "ENI"))).isTrue();
    }

    @Test
    void deficitJamaisNegatifEtNulSansPostes() {
        assertThat(ProfilEnseignantService.deficit(null, 10)).isNull();
        assertThat(ProfilEnseignantService.deficit(12, 10)).isEqualTo(2);
        assertThat(ProfilEnseignantService.deficit(8, 10)).isEqualTo(0);
    }

    @Test
    void priseDeServiceCoherente() {
        assertThat(ProfilEnseignantService.verifier(requete(LocalDate.of(1990, 1, 1), LocalDate.of(2015, 10, 1)))).isNull();
        assertThat(ProfilEnseignantService.verifier(requete(LocalDate.of(1990, 1, 1), LocalDate.of(1989, 10, 1)))).isNotNull();
        assertThat(ProfilEnseignantService.verifier(requete(LocalDate.of(2000, 1, 1), LocalDate.of(2012, 10, 1)))).isNotNull();
        assertThat(ProfilEnseignantService.verifier(requete(null, LocalDate.of(2012, 10, 1)))).isNull();
    }
}
