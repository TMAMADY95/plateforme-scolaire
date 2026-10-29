package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.plateforme.scolaire.domain.ReinitialisationMotDePasse;
import com.plateforme.scolaire.domain.Utilisateur;
import com.plateforme.scolaire.repository.ReinitialisationMotDePasseRepository;
import com.plateforme.scolaire.repository.UtilisateurRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

class ReinitialisationMotDePasseServiceTest {

    private static final Instant MAINTENANT = Instant.parse("2026-10-29T10:00:00Z");

    private final UtilisateurRepository utilisateurRepository = mock(UtilisateurRepository.class);
    private final ReinitialisationMotDePasseRepository repository = mock(ReinitialisationMotDePasseRepository.class);
    private final PasswordEncoder encodeur = mock(PasswordEncoder.class);
    private final EnvoyeurMail mail = mock(EnvoyeurMail.class);
    private final ReinitialisationMotDePasseService service = new ReinitialisationMotDePasseService(
            utilisateurRepository, repository, encodeur, mail, mock(AuditService.class),
            Clock.fixed(MAINTENANT, ZoneOffset.UTC), "http://site.test/");

    private Utilisateur awa;

    @BeforeEach
    void preparer() {
        awa = new Utilisateur();
        awa.setId(3L);
        awa.setEmail("awa@example.org");
        awa.setPrenom("Awa");
        awa.setActif(true);
        awa.setEchecsConnexion(4);
        awa.setVerrouilleJusquA(MAINTENANT.plusSeconds(600));
        awa.setDoitChangerMotDePasse(true);
        when(utilisateurRepository.findByEmailIgnoreCase("awa@example.org")).thenReturn(Optional.of(awa));
        when(encodeur.encode(anyString())).thenReturn("HASH");
    }

    private ReinitialisationMotDePasse demandeValide(String jeton) {
        ReinitialisationMotDePasse d = new ReinitialisationMotDePasse();
        d.setUtilisateur(awa);
        d.setJetonHash(ReinitialisationMotDePasseService.empreinte(jeton));
        d.setExpireLe(MAINTENANT.plusSeconds(600));
        when(repository.findByJetonHash(d.getJetonHash())).thenReturn(Optional.of(d));
        return d;
    }

    // ---------- Demande ----------

    @Test
    void uneAdresseInconnueNeFaitRienEtNeLeRevelePas() {
        service.demander("inconnu@example.org");

        verify(repository, never()).save(any());
        verify(mail, never()).envoyer(any(), any(), any());
    }

    @Test
    void unCompteDesactiveNeRecoitRien() {
        awa.setActif(false);

        service.demander("awa@example.org");

        verify(mail, never()).envoyer(any(), any(), any());
    }

    @Test
    void uneDemandeValideStockeSeulementLEmpreinteEtEnvoieLeLien() {
        service.demander("  AWA@example.org".trim().toLowerCase());

        ArgumentCaptor<ReinitialisationMotDePasse> demande = ArgumentCaptor.forClass(ReinitialisationMotDePasse.class);
        verify(repository).save(demande.capture());
        ArgumentCaptor<String> texte = ArgumentCaptor.forClass(String.class);
        verify(mail).envoyer(eq("awa@example.org"), anyString(), texte.capture());

        String lien = texte.getValue().lines().filter(l -> l.startsWith("http://site.test/reinitialisation?jeton="))
                .findFirst().orElseThrow();
        String jeton = lien.substring(lien.indexOf("jeton=") + 6);
        assertThat(jeton).hasSizeGreaterThanOrEqualTo(40);
        assertThat(demande.getValue().getJetonHash()).isNotEqualTo(jeton)
                .isEqualTo(ReinitialisationMotDePasseService.empreinte(jeton));
        assertThat(demande.getValue().getExpireLe()).isEqualTo(MAINTENANT.plusSeconds(30 * 60));
    }

    @Test
    void uneDemandeTropRapprocheeNEnvoiePasUnDeuxiemeMail() {
        when(repository.existsByUtilisateurIdAndCreatedAtAfter(eq(3L), any())).thenReturn(true);

        service.demander("awa@example.org");

        verify(mail, never()).envoyer(any(), any(), any());
        verify(repository, never()).save(any());
    }

    @Test
    void unePanneDEmailNeSeVoitPasDeLUtilisateur() {
        doThrow(new RuntimeException("SMTP en panne")).when(mail).envoyer(any(), any(), any());

        service.demander("awa@example.org");

        verify(repository).save(any());
    }

    // ---------- Réinitialisation ----------

    @Test
    void unLienValideChangeLeMotDePasseRevoqueLesSessionsEtLeveLeVerrouillage() {
        ReinitialisationMotDePasse demande = demandeValide("abc");

        service.reinitialiser("abc", "NouveauMotDePasse1!");

        assertThat(awa.getMotDePasseHash()).isEqualTo("HASH");
        assertThat(awa.isDoitChangerMotDePasse()).isFalse();
        assertThat(awa.getMotDePasseModifieLe()).isEqualTo(MAINTENANT);
        assertThat(awa.getEchecsConnexion()).isZero();
        assertThat(awa.getVerrouilleJusquA()).isNull();
        assertThat(demande.getUtiliseLe()).isEqualTo(MAINTENANT);
    }

    @Test
    void unLienDejaUtiliseEstRefuse() {
        demandeValide("abc").setUtiliseLe(MAINTENANT.minusSeconds(5));

        assertThatThrownBy(() -> service.reinitialiser("abc", "NouveauMotDePasse1!"))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("invalide ou a expiré");
        verify(encodeur, never()).encode(anyString());
    }

    @Test
    void unLienExpireEstRefuse() {
        demandeValide("abc").setExpireLe(MAINTENANT.minusSeconds(1));

        assertThatThrownBy(() -> service.reinitialiser("abc", "NouveauMotDePasse1!"))
                .hasMessageContaining("invalide ou a expiré");
    }

    @Test
    void unLienInconnuEstRefuse() {
        assertThatThrownBy(() -> service.reinitialiser("n-importe-quoi", "NouveauMotDePasse1!"))
                .hasMessageContaining("invalide ou a expiré");
    }

    @Test
    void unCompteDesactiveDepuisNePeutPlusReinitialiser() {
        demandeValide("abc");
        awa.setActif(false);

        assertThatThrownBy(() -> service.reinitialiser("abc", "NouveauMotDePasse1!"))
                .hasMessageContaining("invalide ou a expiré");
    }
}
