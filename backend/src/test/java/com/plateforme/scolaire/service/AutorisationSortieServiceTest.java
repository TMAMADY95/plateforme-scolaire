package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.plateforme.scolaire.domain.AutorisationSortie;
import com.plateforme.scolaire.domain.Eleve;
import com.plateforme.scolaire.domain.Etablissement;
import com.plateforme.scolaire.domain.Utilisateur;
import com.plateforme.scolaire.repository.AutorisationSortieRepository;
import com.plateforme.scolaire.repository.EleveRepository;
import com.plateforme.scolaire.repository.EtablissementRepository;
import com.plateforme.scolaire.repository.InscriptionRepository;
import com.plateforme.scolaire.repository.UtilisateurRepository;
import com.plateforme.scolaire.security.PerimetreService;
import com.plateforme.scolaire.security.UtilisateurPrincipal;
import com.plateforme.scolaire.web.dto.ReponseRequest;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

class AutorisationSortieServiceTest {

    private final AutorisationSortieRepository sorties = mock(AutorisationSortieRepository.class);
    private final UtilisateurRepository utilisateurs = mock(UtilisateurRepository.class);
    private final PerimetreService perimetre = mock(PerimetreService.class);
    private AutorisationSortieService service;
    private AutorisationSortie demande;

    @BeforeEach
    void preparer() {
        service = new AutorisationSortieService(sorties, mock(EleveRepository.class), mock(EtablissementRepository.class),
                mock(InscriptionRepository.class), utilisateurs, perimetre, mock(AuditService.class),
                Clock.fixed(Instant.parse("2026-10-30T08:00:00Z"), ZoneOffset.UTC));
        Etablissement e = new Etablissement();
        e.setId(4L);
        Eleve eleve = new Eleve();
        eleve.setId(9L);
        eleve.setNom("Diallo");
        eleve.setPrenom("Aminata");
        demande = new AutorisationSortie();
        demande.setId(3L);
        demande.setEleve(eleve);
        demande.setEtablissement(e);
        demande.setDateSortie(LocalDate.of(2026, 10, 31));
        demande.setHeureSortie(LocalTime.of(10, 0));
        demande.setMotif("Rendez-vous médical");
        when(sorties.findById(3L)).thenReturn(Optional.of(demande));
        when(perimetre.utilisateurCourant()).thenReturn(new UtilisateurPrincipal(7L, "d@x.org", "h", true, List.of()));
        when(utilisateurs.getReferenceById(7L)).thenReturn(new Utilisateur());
    }

    @Test
    void laDirectionAccepteUneDemande() {
        when(perimetre.peutGererEtablissement(4L)).thenReturn(true);

        assertThat(service.decider(3L, new ReponseRequest(true, "Bon rétablissement")).statut()).isEqualTo("ACCEPTEE");
    }

    @Test
    void uneDemandeDejaTraiteeNePeutPasEtreRedecidee() {
        when(perimetre.peutGererEtablissement(4L)).thenReturn(true);
        demande.setStatut("REFUSEE");

        assertThatThrownBy(() -> service.decider(3L, new ReponseRequest(true, null))).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void uneAutreEcoleNePeutPasDecider() {
        when(perimetre.peutGererEtablissement(4L)).thenReturn(false);

        assertThatThrownBy(() -> service.decider(3L, new ReponseRequest(true, null))).isInstanceOf(AccessDeniedException.class);
    }
}
