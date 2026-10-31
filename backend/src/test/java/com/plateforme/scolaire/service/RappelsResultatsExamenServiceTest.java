package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.plateforme.scolaire.domain.AffectationRole;
import com.plateforme.scolaire.domain.CandidatExamen;
import com.plateforme.scolaire.domain.Etablissement;
import com.plateforme.scolaire.domain.ExamenNational;
import com.plateforme.scolaire.domain.Role;
import com.plateforme.scolaire.domain.Utilisateur;
import com.plateforme.scolaire.repository.AffectationRoleRepository;
import com.plateforme.scolaire.repository.AlerteEnvoyeeRepository;
import com.plateforme.scolaire.repository.CandidatExamenRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class RappelsResultatsExamenServiceTest {

    private static final Instant MAINTENANT = Instant.parse("2026-10-30T07:00:00Z");

    private final CandidatExamenRepository candidats = mock(CandidatExamenRepository.class);
    private final AffectationRoleRepository affectations = mock(AffectationRoleRepository.class);
    private final AlerteEnvoyeeRepository alertes = mock(AlerteEnvoyeeRepository.class);
    private final EnvoyeurMail mail = mock(EnvoyeurMail.class);
    private final EnvoyeurSms sms = mock(EnvoyeurSms.class);
    private final JournalAlertes journal = mock(JournalAlertes.class);
    private final RappelsResultatsExamenService service = new RappelsResultatsExamenService(candidats, affectations,
            alertes, mail, sms, journal, Clock.fixed(MAINTENANT, ZoneOffset.UTC), true, "http://front");

    private void situation() {
        ExamenNational examen = new ExamenNational();
        examen.setId(4L);
        examen.setLibelle("BEPC");
        Etablissement etab = new Etablissement();
        etab.setId(7L);
        etab.setNom("École 7");
        // deux candidats du même établissement : un seul rappel
        CandidatExamen c1 = new CandidatExamen();
        c1.setExamen(examen);
        c1.setEtablissement(etab);
        CandidatExamen c2 = new CandidatExamen();
        c2.setExamen(examen);
        c2.setEtablissement(etab);
        when(candidats.sansResultats(LocalDate.of(2026, 10, 16))).thenReturn(List.of(c1, c2));

        Utilisateur u = new Utilisateur();
        u.setEmail("direction@ecole7.gn");
        AffectationRole a = new AffectationRole();
        a.setUtilisateur(u);
        a.setRole(Role.DIRECTION_ETABLISSEMENT);
        a.setEtablissement(etab);
        when(affectations.directionsDe(any())).thenReturn(List.of(a));
    }

    @Test
    void laDirectionEstRappeleeUneSeuleFoisParExamen() {
        situation();

        assertThat(service.rappeler()).isEqualTo(1);
        verify(mail).envoyer(eq("direction@ecole7.gn"), anyString(), anyString());
        verify(journal).noter(eq("RESULTATS_EXAMEN"), eq("EMAIL"), eq("direction@ecole7.gn"), anyString(), eq(true));
    }

    @Test
    void unRappelDejaEnvoyeCetteSemaineNEstPasRepete() {
        situation();
        when(alertes.existsByTypeAndResumeStartingWithAndReussieTrueAndEnvoyeeLeAfter(
                eq("RESULTATS_EXAMEN"), eq("EXA#4-7 "), any())).thenReturn(true);

        assertThat(service.rappeler()).isZero();
        verify(mail, never()).envoyer(anyString(), anyString(), anyString());
    }

    @Test
    void sansRetardAucunRappel() {
        when(candidats.sansResultats(any())).thenReturn(List.of());

        assertThat(service.rappeler()).isZero();
    }
}
