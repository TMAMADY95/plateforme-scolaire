package com.plateforme.scolaire.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.plateforme.scolaire.domain.Eleve;
import com.plateforme.scolaire.domain.Inscription;
import com.plateforme.scolaire.domain.LienParental;
import com.plateforme.scolaire.domain.Presence;
import com.plateforme.scolaire.domain.Utilisateur;
import com.plateforme.scolaire.repository.AlerteEnvoyeeRepository;
import com.plateforme.scolaire.repository.LienParentalRepository;
import com.plateforme.scolaire.repository.PresenceRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class AbsencesParentsServiceTest {

    private static final LocalDate JOUR = LocalDate.of(2026, 10, 12);

    private final PresenceRepository presences = mock(PresenceRepository.class);
    private final LienParentalRepository liens = mock(LienParentalRepository.class);
    private final AlerteEnvoyeeRepository alertes = mock(AlerteEnvoyeeRepository.class);
    private final EnvoyeurSms sms = mock(EnvoyeurSms.class);
    private final JournalAlertes journal = mock(JournalAlertes.class);

    private AbsencesParentsService service(boolean actif) {
        return new AbsencesParentsService(presences, liens, alertes, sms, journal, actif);
    }

    private void absence(String statut, boolean justifie) {
        Eleve eleve = new Eleve();
        eleve.setId(2L);
        eleve.setNom("Diallo");
        eleve.setPrenom("Aminata");
        Inscription i = new Inscription();
        i.setEleve(eleve);
        Presence p = new Presence();
        p.setId(9L);
        p.setInscription(i);
        p.setStatut(statut);
        p.setJustifie(justifie);
        when(presences.findByInscriptionClasseIdAndDateSeance(1L, JOUR)).thenReturn(List.of(p));
        Utilisateur parent = new Utilisateur();
        parent.setEmail("parent@exemple.gn");
        parent.setTelephone("+224620000000");
        LienParental lien = new LienParental();
        lien.setEleve(eleve);
        lien.setUtilisateur(parent);
        when(liens.findByEleveId(2L)).thenReturn(List.of(lien));
    }

    @Test
    void uneAbsenceNonJustifieePrevientLesParents() {
        absence("ABSENT", false);
        when(sms.envoyer(eq("+224620000000"), anyString())).thenReturn(true);

        org.assertj.core.api.Assertions.assertThat(service(true).prevenir(1L, JOUR)).isEqualTo(1);
        verify(journal).noter(eq("ABSENCE"), eq("SMS"), eq("parent@exemple.gn"), anyString(), eq(true));
    }

    @Test
    void uneAbsenceDejaSignaleeNEstPasRenvoyee() {
        absence("ABSENT", false);
        when(alertes.existsByTypeAndResumeStartingWithAndReussieTrue("ABSENCE", "ABS#9 ")).thenReturn(true);

        org.assertj.core.api.Assertions.assertThat(service(true).prevenir(1L, JOUR)).isZero();
        verify(sms, never()).envoyer(any(), any());
    }

    @Test
    void retardAbsenceJustifieeOuFonctionDesactiveeNEnvoientRien() {
        absence("RETARD", false);
        service(true).prevenir(1L, JOUR);
        absence("ABSENT", true);
        service(true).prevenir(1L, JOUR);
        absence("ABSENT", false);
        service(false).prevenir(1L, JOUR);

        verify(sms, never()).envoyer(any(), any());
        verify(journal, never()).noter(anyString(), anyString(), anyString(), anyString(), anyBoolean());
    }
}
