package com.plateforme.scolaire.service;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.plateforme.scolaire.repository.LienParentalRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class NotificationAnnoncesTest {

    private final LienParentalRepository liens = mock(LienParentalRepository.class);
    private final EnvoyeurMail mail = mock(EnvoyeurMail.class);
    private final AnnoncePubliee evenement = new AnnoncePubliee(5L, "6A", "Sortie scolaire", "Moussa Ba");

    @Test
    void chaqueParentRecoitUnEmailAvecLeTitreSeulement() {
        when(liens.emailsParentsDeLaClasse(5L)).thenReturn(List.of("a@exemple.org", "b@exemple.org"));
        new NotificationAnnonces(liens, mail, true, "https://ecole.example.org").notifier(evenement);

        ArgumentCaptor<String> texte = ArgumentCaptor.forClass(String.class);
        verify(mail).envoyer(eq("a@exemple.org"), contains("6A"), texte.capture());
        verify(mail).envoyer(eq("b@exemple.org"), contains("6A"), anyString());
        org.assertj.core.api.Assertions.assertThat(texte.getValue())
                .contains("Sortie scolaire").contains("Moussa Ba").contains("https://ecole.example.org");
    }

    @Test
    void uneAdresseEnEchecNeBloquePasLesSuivantes() {
        when(liens.emailsParentsDeLaClasse(5L)).thenReturn(List.of("a@exemple.org", "b@exemple.org"));
        doThrow(new RuntimeException("SMTP")).when(mail).envoyer(eq("a@exemple.org"), anyString(), anyString());
        new NotificationAnnonces(liens, mail, true, "http://x").notifier(evenement);

        verify(mail).envoyer(eq("b@exemple.org"), anyString(), anyString());
    }

    @Test
    void desactivee_aucunEmailNEstEnvoye() {
        new NotificationAnnonces(liens, mail, false, "http://x").notifier(evenement);
        verify(mail, never()).envoyer(anyString(), anyString(), anyString());
        verify(liens, never()).emailsParentsDeLaClasse(5L);
    }

    @Test
    void uneErreurDeLectureNeRemontePas() {
        when(liens.emailsParentsDeLaClasse(5L)).thenThrow(new RuntimeException("base"));
        new NotificationAnnonces(liens, mail, true, "http://x").notifier(evenement);
        verify(mail, never()).envoyer(anyString(), anyString(), anyString());
    }
}
