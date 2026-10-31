package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.plateforme.scolaire.domain.AffectationRole;
import com.plateforme.scolaire.domain.Etablissement;
import com.plateforme.scolaire.domain.PlanAction;
import com.plateforme.scolaire.domain.Role;
import com.plateforme.scolaire.domain.Utilisateur;
import com.plateforme.scolaire.repository.AffectationRoleRepository;
import com.plateforme.scolaire.repository.PlanActionRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RappelsPlansActionServiceTest {

    private static final Instant MAINTENANT = Instant.parse("2026-10-30T07:00:00Z");

    private final PlanActionRepository plans = mock(PlanActionRepository.class);
    private final AffectationRoleRepository affectations = mock(AffectationRoleRepository.class);
    private final EnvoyeurMail mail = mock(EnvoyeurMail.class);
    private final EnvoyeurSms sms = mock(EnvoyeurSms.class);
    private final JournalAlertes journal = mock(JournalAlertes.class);
    private RappelsPlansActionService service;

    @BeforeEach
    void preparer() {
        service = new RappelsPlansActionService(plans, affectations, mail, sms, journal,
                Clock.fixed(MAINTENANT, ZoneOffset.UTC), true, "http://front");
    }

    private static PlanAction plan(LocalDate echeance) {
        Etablissement e = new Etablissement();
        e.setId(7L);
        e.setNom("École 7");
        PlanAction p = new PlanAction();
        p.setEtablissement(e);
        p.setDescription("Réparer le toit");
        p.setEcheance(echeance);
        return p;
    }

    private static AffectationRole direction(String email, String tel) {
        Utilisateur u = new Utilisateur();
        u.setId(1L);
        u.setEmail(email);
        u.setTelephone(tel);
        AffectationRole a = new AffectationRole();
        a.setUtilisateur(u);
        a.setRole(Role.DIRECTION_ETABLISSEMENT);
        Etablissement e = new Etablissement();
        e.setId(7L);
        a.setEtablissement(e);
        return a;
    }

    @Test
    void lADirectionEstPrevenueParEmailEtSmsEtLeRappelEstMemorise() {
        PlanAction p = plan(LocalDate.of(2026, 10, 29));
        when(plans.aRappeler(any(), any())).thenReturn(List.of(p));
        when(affectations.directionsDe(any())).thenReturn(List.of(direction("dir@exemple.org", "620000000")));
        when(sms.envoyer(anyString(), anyString())).thenReturn(true);

        assertThat(service.rappeler()).isEqualTo(1);

        verify(mail).envoyer(eq("dir@exemple.org"), anyString(), anyString());
        verify(journal).noter(eq("PLAN_ACTION"), eq("EMAIL"), eq("dir@exemple.org"), anyString(), eq(true));
        verify(journal).noter(eq("PLAN_ACTION"), eq("SMS"), eq("620000000"), anyString(), eq(true));
        assertThat(p.getDernierRappel()).isEqualTo(MAINTENANT);
    }

    @Test
    void sansPlanARappelerRienNEstEnvoye() {
        when(plans.aRappeler(any(), any())).thenReturn(List.of());

        assertThat(service.rappeler()).isZero();
        verify(mail, never()).envoyer(anyString(), anyString(), anyString());
        verify(journal, never()).noter(anyString(), anyString(), anyString(), anyString(), anyBoolean());
    }

    @Test
    void sansDirectionJoignableLePlanNEstPasMarqueCommeRappele() {
        PlanAction p = plan(LocalDate.of(2026, 11, 1));
        when(plans.aRappeler(any(), any())).thenReturn(List.of(p));
        when(affectations.directionsDe(any())).thenReturn(List.of());

        assertThat(service.rappeler()).isZero();
        assertThat(p.getDernierRappel()).isNull();
    }
}
