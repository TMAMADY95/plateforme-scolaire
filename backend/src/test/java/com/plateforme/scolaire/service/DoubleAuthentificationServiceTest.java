package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.plateforme.scolaire.domain.CodeVerification;
import com.plateforme.scolaire.domain.Perimetre;
import com.plateforme.scolaire.domain.Role;
import com.plateforme.scolaire.repository.CodeVerificationRepository;
import com.plateforme.scolaire.security.UtilisateurPrincipal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class DoubleAuthentificationServiceTest {

    private final CodeVerificationRepository codes = mock(CodeVerificationRepository.class);
    private final EnvoyeurMail mail = mock(EnvoyeurMail.class);
    private final BCryptPasswordEncoder encodeur = new BCryptPasswordEncoder(4);
    private DoubleAuthentificationService service;
    private CodeVerification stocke;

    @BeforeEach
    void preparer() {
        Clock horloge = Clock.fixed(Instant.parse("2026-10-30T10:00:00Z"), ZoneOffset.UTC);
        service = new DoubleAuthentificationService(codes, encodeur, mail, horloge, true);
        when(codes.findById(1L)).thenAnswer(i -> Optional.ofNullable(stocke));
        when(codes.save(any())).thenAnswer(i -> {
            stocke = i.getArgument(0);
            return stocke;
        });
    }

    private static String codeEnvoye(EnvoyeurMail mail) {
        ArgumentCaptor<String> texte = forClass(String.class);
        verify(mail).envoyer(eq("min@exemple.org"), anyString(), texte.capture());
        return texte.getValue().replaceAll("(?s).*connexion : (\\d{6}).*", "$1");
    }

    @Test
    void leBonCodeNeServQuUneFois() {
        service.envoyerCode(1L, "min@exemple.org");
        String code = codeEnvoye(mail);

        assertThat(code).matches("\\d{6}");
        assertThat(service.verifier(1L, code)).isTrue();
        stocke = null; // supprimé après usage
        assertThat(service.verifier(1L, code)).isFalse();
    }

    @Test
    void unMauvaisCodeEstRefuseEtCompte() {
        service.envoyerCode(1L, "min@exemple.org");

        assertThat(service.verifier(1L, "000000x")).isFalse();
        assertThat(stocke.getEssais()).isEqualTo(1);
    }

    @Test
    void apresCinqEssaisMemeLeBonCodeEstRefuse() {
        service.envoyerCode(1L, "min@exemple.org");
        String code = codeEnvoye(mail);
        stocke.setEssais(DoubleAuthentificationService.ESSAIS_MAX);

        assertThat(service.verifier(1L, code)).isFalse();
    }

    @Test
    void seulsLeMinistereEtLesDirectionsRegionalesSontConcernes() {
        var ministere = new UtilisateurPrincipal(1L, "m@x.org", "h", true,
                List.of(new UtilisateurPrincipal.AffectationInfo(Role.MINISTERE, Perimetre.NATIONAL, null, null)));
        var enseignant = new UtilisateurPrincipal(2L, "e@x.org", "h", true,
                List.of(new UtilisateurPrincipal.AffectationInfo(Role.ENSEIGNANT, Perimetre.ETABLISSEMENT, null, 5L)));

        assertThat(service.requise(ministere)).isTrue();
        assertThat(service.requise(enseignant)).isFalse();
    }
}
