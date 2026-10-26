package com.plateforme.scolaire.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.plateforme.scolaire.domain.Utilisateur;
import com.plateforme.scolaire.repository.UtilisateurRepository;
import com.plateforme.scolaire.service.SecuriteComptesService;
import com.plateforme.scolaire.web.dto.ChangementMotDePasseRequest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

/** Verrouillage après échecs et changement de mot de passe (3 échecs verrouillent 15 minutes dans ces tests). */
class SecuriteComptesServiceTest {

    private static final Instant MAINTENANT = Instant.parse("2026-10-26T10:00:00Z");

    private final UtilisateurRepository utilisateurRepository = mock(UtilisateurRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final Clock horloge = Clock.fixed(MAINTENANT, ZoneOffset.UTC);
    private final SecuriteComptesService service =
            new SecuriteComptesService(utilisateurRepository, encoder, horloge, 3, 15);

    private Utilisateur utilisateur;

    @BeforeEach
    void preparer() {
        utilisateur = new Utilisateur();
        utilisateur.setId(7L);
        utilisateur.setEmail("u@example.org");
        utilisateur.setMotDePasseHash("hash-actuel");
        utilisateur.setDoitChangerMotDePasse(true);
        when(utilisateurRepository.findByEmailIgnoreCase("u@example.org")).thenReturn(Optional.of(utilisateur));
        when(utilisateurRepository.findById(7L)).thenReturn(Optional.of(utilisateur));
        when(encoder.matches("actuel-12345", "hash-actuel")).thenReturn(true);
        when(encoder.encode("nouveau-12345")).thenReturn("hash-nouveau");
    }

    @Test
    void unEchecIncrementeLeCompteurSansVerrouiller() {
        service.enregistrerEchec("u@example.org");
        service.enregistrerEchec(" u@example.org ");
        assertThat(utilisateur.getEchecsConnexion()).isEqualTo(2);
        assertThat(utilisateur.getVerrouilleJusquA()).isNull();
    }

    @Test
    void leSeuilDEchecsVerrouilleLeCompteQuinzeMinutes() {
        service.enregistrerEchec("u@example.org");
        service.enregistrerEchec("u@example.org");
        service.enregistrerEchec("u@example.org");
        assertThat(utilisateur.getVerrouilleJusquA()).isEqualTo(MAINTENANT.plusSeconds(15 * 60));
        assertThat(utilisateur.getEchecsConnexion()).isZero();
    }

    @Test
    void uneAdresseInconnueNeProvoqueAucuneErreur() {
        service.enregistrerEchec("inconnu@example.org");
        assertThat(utilisateur.getEchecsConnexion()).isZero();
    }

    @Test
    void uneConnexionReussieRemetLeCompteurAZero() {
        utilisateur.setEchecsConnexion(2);
        service.enregistrerSucces(7L);
        assertThat(utilisateur.getEchecsConnexion()).isZero();
        assertThat(utilisateur.getVerrouilleJusquA()).isNull();
    }

    @Test
    void leChangementDeMotDePasseMetAJourLeCompteEtRevoqueLesAnciensJetons() {
        service.changerMotDePasse(7L, new ChangementMotDePasseRequest("actuel-12345", "nouveau-12345"));
        assertThat(utilisateur.getMotDePasseHash()).isEqualTo("hash-nouveau");
        assertThat(utilisateur.isDoitChangerMotDePasse()).isFalse();
        assertThat(utilisateur.getMotDePasseModifieLe()).isEqualTo(MAINTENANT);
    }

    @Test
    void unMauvaisMotDePasseActuelEstRefuseEtCompteCommeUnEchec() {
        assertThatThrownBy(() -> service.changerMotDePasse(7L,
                new ChangementMotDePasseRequest("mauvais-12345", "nouveau-12345")))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode().value()).isEqualTo(400));
        assertThat(utilisateur.getEchecsConnexion()).isEqualTo(1);
        assertThat(utilisateur.getMotDePasseHash()).isEqualTo("hash-actuel");
    }

    @Test
    void lesTentativesRepeteesDeChangementVerrouillentLeCompte() {
        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> service.changerMotDePasse(7L,
                    new ChangementMotDePasseRequest("mauvais-12345", "nouveau-12345")))
                    .isInstanceOf(ResponseStatusException.class);
        }
        assertThat(utilisateur.getVerrouilleJusquA()).isNotNull();
    }

    @Test
    void leNouveauMotDePasseDoitDifferer() {
        assertThatThrownBy(() -> service.changerMotDePasse(7L,
                new ChangementMotDePasseRequest("actuel-12345", "actuel-12345")))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode().value()).isEqualTo(400));
        assertThat(utilisateur.getMotDePasseHash()).isEqualTo("hash-actuel");
    }
}
