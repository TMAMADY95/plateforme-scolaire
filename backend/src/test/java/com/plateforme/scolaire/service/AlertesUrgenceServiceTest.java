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
import com.plateforme.scolaire.domain.AlerteUrgenceEtat;
import com.plateforme.scolaire.domain.Etablissement;
import com.plateforme.scolaire.domain.Perimetre;
import com.plateforme.scolaire.domain.Role;
import com.plateforme.scolaire.domain.Utilisateur;
import com.plateforme.scolaire.domain.ZoneGeographique;
import com.plateforme.scolaire.repository.AffectationRoleRepository;
import com.plateforme.scolaire.repository.AlerteUrgenceEtatRepository;
import com.plateforme.scolaire.repository.EtablissementRepository;
import com.plateforme.scolaire.web.dto.ConformiteDto;
import com.plateforme.scolaire.web.dto.LigneConformiteDto;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AlertesUrgenceServiceTest {

    private static final Instant MAINTENANT = Instant.parse("2026-10-30T07:00:00Z");

    private final ConformiteService conformite = mock(ConformiteService.class);
    private final AffectationRoleRepository affectations = mock(AffectationRoleRepository.class);
    private final EtablissementRepository etablissements = mock(EtablissementRepository.class);
    private final AlerteUrgenceEtatRepository etats = mock(AlerteUrgenceEtatRepository.class);
    private final EnvoyeurMail mail = mock(EnvoyeurMail.class);
    private final EnvoyeurSms sms = mock(EnvoyeurSms.class);
    private final JournalAlertes journal = mock(JournalAlertes.class);
    private AlertesUrgenceService service;

    @BeforeEach
    void preparer() {
        service = new AlertesUrgenceService(conformite, affectations, etablissements, etats, mail, sms, journal,
                Clock.fixed(MAINTENANT, ZoneOffset.UTC), true, "http://front");
    }

    private static LigneConformiteDto ligne(long id, boolean urgent) {
        return new LigneConformiteDto(id, "C" + id, "École " + id, "PUBLIC", List.of(), "ACTIF", 5L, "Kindia",
                "AUTORISE", null, null, null, null, false, false, 0, 0, null, false, urgent, urgent ? 4 : 0,
                urgent ? List.of("Jamais inspecté") : List.of());
    }

    private void lignes(LigneConformiteDto... l) {
        when(conformite.nationale(null, null, null, null))
                .thenReturn(new ConformiteDto(new ConformiteDto.Indicateurs(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0), List.of(l)));
    }

    private static AffectationRole direction(long compte, String email, Long zoneId) {
        Utilisateur u = new Utilisateur();
        u.setId(compte);
        u.setEmail(email);
        u.setTelephone("620000000");
        AffectationRole a = new AffectationRole();
        a.setUtilisateur(u);
        a.setRole(Role.DIRECTION_REGIONALE);
        a.setPerimetre(Perimetre.ZONE);
        ZoneGeographique z = new ZoneGeographique();
        z.setId(zoneId);
        a.setZone(z);
        return a;
    }

    private static Etablissement etab(long id) {
        Etablissement e = new Etablissement();
        e.setId(id);
        return e;
    }

    @Test
    void unNouvelEtablissementUrgentEstSignaleASaDirectionRegionaleSeulement() {
        lignes(ligne(1, true), ligne(2, true));
        when(affectations.destinataires(any())).thenReturn(List.of(direction(10, "kindia@exemple.org", 5L)));
        when(etablissements.findByZoneEtSousZones(5L)).thenReturn(List.of(etab(1)));
        when(etats.findAll()).thenReturn(List.of());
        when(sms.envoyer(anyString(), anyString())).thenReturn(true);

        int n = service.alerter();

        assertThat(n).isEqualTo(1);
        verify(mail).envoyer(eq("kindia@exemple.org"), anyString(), org.mockito.ArgumentMatchers.contains("École 1"));
        verify(sms).envoyer(eq("620000000"), anyString());
        verify(journal).noter(eq("URGENCE"), eq("EMAIL"), eq("kindia@exemple.org"), anyString(), eq(true));
    }

    @Test
    void unEtablissementDejaSignaleRecemmentNEstPasRenvoye() {
        lignes(ligne(1, true));
        AlerteUrgenceEtat etat = new AlerteUrgenceEtat(1L);
        etat.setUrgent(true);
        etat.setDerniereAlerte(MAINTENANT.minusSeconds(3600L * 24));
        when(affectations.destinataires(any())).thenReturn(List.of(direction(10, "kindia@exemple.org", 5L)));
        when(etablissements.findByZoneEtSousZones(5L)).thenReturn(List.of(etab(1)));
        when(etats.findAll()).thenReturn(List.of(etat));

        assertThat(service.alerter()).isZero();
        verify(mail, never()).envoyer(anyString(), anyString(), anyString());
    }

    @Test
    void apresUneSemaineLEtablissementEstRappele() {
        lignes(ligne(1, true));
        AlerteUrgenceEtat etat = new AlerteUrgenceEtat(1L);
        etat.setUrgent(true);
        etat.setDerniereAlerte(MAINTENANT.minusSeconds(3600L * 24 * 8));
        when(affectations.destinataires(any())).thenReturn(List.of(direction(10, "kindia@exemple.org", 5L)));
        when(etablissements.findByZoneEtSousZones(5L)).thenReturn(List.of(etab(1)));
        when(etats.findAll()).thenReturn(List.of(etat));

        assertThat(service.alerter()).isEqualTo(1);
        assertThat(etat.getDerniereAlerte()).isEqualTo(MAINTENANT);
    }

    @Test
    void unEtablissementQuiRedevientConformeSeraSignaleDeNouveauSiIlRedevientUrgent() {
        lignes(ligne(1, false));
        AlerteUrgenceEtat etat = new AlerteUrgenceEtat(1L);
        etat.setUrgent(true);
        etat.setDerniereAlerte(MAINTENANT.minusSeconds(60));
        when(affectations.destinataires(any())).thenReturn(List.of());
        when(etats.findAll()).thenReturn(List.of(etat));

        service.alerter();

        assertThat(etat.isUrgent()).isFalse();
        when(conformite.nationale(null, null, null, null)).thenReturn(
                new ConformiteDto(new ConformiteDto.Indicateurs(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0), List.of(ligne(1, true))));
        when(affectations.destinataires(any())).thenReturn(List.of(direction(10, "k@exemple.org", 5L)));
        when(etablissements.findByZoneEtSousZones(5L)).thenReturn(List.of(etab(1)));
        assertThat(service.alerter()).isEqualTo(1);
    }
}
