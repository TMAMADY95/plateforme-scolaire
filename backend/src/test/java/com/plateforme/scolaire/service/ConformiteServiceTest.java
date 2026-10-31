package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.plateforme.scolaire.domain.Etablissement;
import com.plateforme.scolaire.web.dto.ConformiteDto;
import com.plateforme.scolaire.web.dto.LigneConformiteDto;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class ConformiteServiceTest {

    private static final LocalDate AUJOURDHUI = LocalDate.of(2026, 10, 30);

    private static Etablissement etablissement(String statut, String autorisation) {
        Etablissement e = new Etablissement();
        e.setId(1L);
        e.setNom("École test");
        e.setCodeOfficiel("T1");
        e.setStatut(statut);
        e.setAutorisationStatut(autorisation);
        return e;
    }

    @Test
    void unEtablissementEnRegleN_estPasUrgent() {
        LigneConformiteDto l = ConformiteService.ligne(etablissement("ACTIF", "AUTORISE"), AUJOURDHUI,
                AUJOURDHUI.minusDays(60), "CONFORME", 0, 0, AUJOURDHUI.minusDays(2));

        assertThat(l.inspecte()).isTrue();
        assertThat(l.nonConforme()).isFalse();
        assertThat(l.sansDonnees()).isFalse();
        assertThat(l.urgent()).isFalse();
        assertThat(l.score()).isZero();
    }

    @Test
    void unProblemeDeSecuriteGraveSuffitPourUneInspectionUrgente() {
        LigneConformiteDto l = ConformiteService.ligne(etablissement("ACTIF", "AUTORISE"), AUJOURDHUI,
                AUJOURDHUI.minusDays(10), "CONFORME", 1, 1, AUJOURDHUI);

        assertThat(l.urgent()).isTrue();
        assertThat(l.motifsUrgence()).contains("Problème de sécurité grave non résolu");
    }

    @Test
    void lesPetitsDefautsSAdditionnent() {
        // jamais inspecté (1) + données non transmises (1) + fonctionne sans autorisation (2) = 4
        LigneConformiteDto l = ConformiteService.ligne(etablissement("ACTIF", "NON_AUTORISE"), AUJOURDHUI,
                null, null, 0, 0, null);

        assertThat(l.score()).isEqualTo(4);
        assertThat(l.urgent()).isTrue();
        assertThat(l.sansDonnees()).isTrue();
        assertThat(l.motifsUrgence()).contains("Jamais inspecté", "Données non transmises", "Fonctionne sans autorisation");
    }

    @Test
    void uneInspectionDePlusDUnAnNeCompteplusCommeInspecte() {
        LigneConformiteDto l = ConformiteService.ligne(etablissement("ACTIF", "AUTORISE"), AUJOURDHUI,
                AUJOURDHUI.minusDays(400), "NON_CONFORME", 0, 0, AUJOURDHUI);

        assertThat(l.inspecte()).isFalse();
        assertThat(l.nonConforme()).isTrue();
        assertThat(l.score()).isEqualTo(3);
    }

    @Test
    void unEtablissementSuspenduNeDoitPasTransmettreDeDonneesEtUnFermeNEstJamaisUrgent() {
        LigneConformiteDto suspendu = ConformiteService.ligne(etablissement("SUSPENDU", "AUTORISE"), AUJOURDHUI,
                AUJOURDHUI.minusDays(5), "CONFORME", 0, 0, null);
        LigneConformiteDto ferme = ConformiteService.ligne(etablissement("FERME", "RETIRE"), AUJOURDHUI,
                null, null, 2, 2, null);

        assertThat(suspendu.sansDonnees()).isFalse();
        assertThat(ferme.urgent()).isFalse();
        assertThat(ferme.score()).isZero();
    }

    @Test
    void lesIndicateursCompterntChaqueSituation() {
        Etablissement prive = etablissement("SUSPENDU", "EN_ATTENTE");
        prive.setSecteur("PRIVE");
        LigneConformiteDto a = ConformiteService.ligne(etablissement("ACTIF", "AUTORISE"), AUJOURDHUI,
                AUJOURDHUI.minusDays(5), "NON_CONFORME", 1, 0, AUJOURDHUI);
        LigneConformiteDto b = ConformiteService.ligne(prive, AUJOURDHUI, null, null, 0, 0, null);

        ConformiteDto.Indicateurs i = ConformiteService.indicateurs(List.of(a, b));

        assertThat(i.total()).isEqualTo(2);
        assertThat(i.publics()).isEqualTo(1);
        assertThat(i.prives()).isEqualTo(1);
        assertThat(i.autorises()).isEqualTo(1);
        assertThat(i.autorisationEnAttente()).isEqualTo(1);
        assertThat(i.nonConformes()).isEqualTo(1);
        assertThat(i.inspectes()).isEqualTo(1);
        assertThat(i.avecProblemeSecurite()).isEqualTo(1);
        assertThat(i.fermes()).isEqualTo(1);
    }
}
