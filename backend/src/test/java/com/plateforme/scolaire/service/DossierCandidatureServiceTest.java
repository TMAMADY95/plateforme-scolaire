package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;

class DossierCandidatureServiceTest {

    @Test
    void leDiplomePrecedentNEstExigeQuePourLeBepcEtLeBac() {
        assertThat(DossierCandidatureService.diplomeRequis("NATIONAL")).isTrue();
        assertThat(DossierCandidatureService.diplomeRequis("REGION")).isTrue();
        assertThat(DossierCandidatureService.diplomeRequis("PREFECTURE")).isFalse();
        assertThat(DossierCandidatureService.diplomeRequis(null)).isFalse();
    }

    @Test
    void ilManqueLesPiecesRequisesNonFournies() {
        assertThat(DossierCandidatureService.manques("PREFECTURE", Set.of())).hasSize(3);
        assertThat(DossierCandidatureService.manques("NATIONAL", Set.of())).hasSize(4);
        assertThat(DossierCandidatureService.manques("NATIONAL", Set.of("DEMANDE", "ACTE_NAISSANCE", "ATTESTATION")))
                .containsExactly("Copie légalisée du diplôme précédent");
    }

    @Test
    void unCandidatLibreNAPasDAttestationDInscription() {
        assertThat(DossierCandidatureService.manques("NATIONAL", Set.of(), true)).hasSize(3)
                .doesNotContain("Attestation d'inscription de l'établissement d'origine");
        assertThat(DossierCandidatureService.manques("NATIONAL", Set.of("DEMANDE", "ACTE_NAISSANCE", "DIPLOME"), true)).isEmpty();
    }

    @Test
    void unDossierDeCeeEstCompletSansDiplome() {
        assertThat(DossierCandidatureService.manques("PREFECTURE", Set.of("DEMANDE", "ACTE_NAISSANCE", "ATTESTATION"))).isEmpty();
    }
}
