package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.plateforme.scolaire.domain.Etablissement;
import com.plateforme.scolaire.domain.Eleve;
import com.plateforme.scolaire.domain.Inscription;
import com.plateforme.scolaire.domain.JustificationAbsence;
import com.plateforme.scolaire.domain.Presence;
import com.plateforme.scolaire.domain.Utilisateur;
import com.plateforme.scolaire.repository.JustificationAbsenceRepository;
import com.plateforme.scolaire.repository.PresenceRepository;
import com.plateforme.scolaire.repository.UtilisateurRepository;
import com.plateforme.scolaire.security.PerimetreService;
import com.plateforme.scolaire.security.UtilisateurPrincipal;
import com.plateforme.scolaire.web.dto.DecisionJustificationRequest;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class JustificationAbsenceServiceTest {

    private final JustificationAbsenceRepository justifications = mock(JustificationAbsenceRepository.class);
    private final PresenceRepository presences = mock(PresenceRepository.class);
    private final UtilisateurRepository utilisateurs = mock(UtilisateurRepository.class);
    private final PerimetreService perimetre = mock(PerimetreService.class);
    private JustificationAbsenceService service;
    private JustificationAbsence demande;
    private Presence presence;

    @BeforeEach
    void preparer() {
        service = new JustificationAbsenceService(justifications, presences, utilisateurs, perimetre,
                mock(AuditService.class), Clock.fixed(Instant.parse("2026-10-30T08:00:00Z"), ZoneOffset.UTC));
        Etablissement e = new Etablissement();
        e.setId(4L);
        Eleve eleve = new Eleve();
        eleve.setId(9L);
        eleve.setNom("Diallo");
        eleve.setPrenom("Aminata");
        Inscription i = new Inscription();
        i.setEleve(eleve);
        i.setEtablissement(e);
        presence = new Presence();
        presence.setId(5L);
        presence.setInscription(i);
        presence.setStatut("ABSENT");
        presence.setDateSeance(LocalDate.of(2026, 10, 28));
        demande = new JustificationAbsence();
        demande.setPresence(presence);
        demande.setMotif("Paludisme");
        when(justifications.findById(1L)).thenReturn(Optional.of(demande));
        when(perimetre.utilisateurCourant()).thenReturn(new UtilisateurPrincipal(7L, "d@x.org", "h", true, List.of()));
        when(utilisateurs.getReferenceById(7L)).thenReturn(new Utilisateur());
    }

    @Test
    void accepterJustifieLAbsenceEtRetientLeMotif() {
        when(perimetre.peutGererEtablissement(4L)).thenReturn(true);

        service.decider(1L, new DecisionJustificationRequest(true, null));

        assertThat(demande.getStatut()).isEqualTo("ACCEPTEE");
        assertThat(presence.isJustifie()).isTrue();
        assertThat(presence.getMotif()).isEqualTo("Paludisme");
    }

    @Test
    void refuserLaisseLAbsenceNonJustifiee() {
        when(perimetre.peutGererEtablissement(4L)).thenReturn(true);

        service.decider(1L, new DecisionJustificationRequest(false, "Pas de certificat"));

        assertThat(demande.getStatut()).isEqualTo("REFUSEE");
        assertThat(presence.isJustifie()).isFalse();
    }

    @Test
    void uneAutreEcoleNePeutPasDecider() {
        when(perimetre.peutGererEtablissement(4L)).thenReturn(false);

        assertThatThrownBy(() -> service.decider(1L, new DecisionJustificationRequest(true, null)))
                .isInstanceOf(AccessDeniedException.class);
    }
}
