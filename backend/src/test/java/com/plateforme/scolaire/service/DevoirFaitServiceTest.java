package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.plateforme.scolaire.domain.Classe;
import com.plateforme.scolaire.domain.Devoir;
import com.plateforme.scolaire.domain.DevoirFait;
import com.plateforme.scolaire.domain.Eleve;
import com.plateforme.scolaire.domain.Enseignant;
import com.plateforme.scolaire.domain.Inscription;
import com.plateforme.scolaire.domain.Matiere;
import com.plateforme.scolaire.domain.Utilisateur;
import com.plateforme.scolaire.repository.DevoirFaitRepository;
import com.plateforme.scolaire.repository.DevoirRepository;
import com.plateforme.scolaire.repository.EleveRepository;
import com.plateforme.scolaire.repository.InscriptionRepository;
import com.plateforme.scolaire.repository.LienParentalRepository;
import com.plateforme.scolaire.security.PerimetreService;
import com.plateforme.scolaire.security.UtilisateurPrincipal;
import com.plateforme.scolaire.web.dto.DevoirDto;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

class DevoirFaitServiceTest {

    private static final long CLASSE = 5L;
    private static final long DEVOIR = 9L;
    private static final long ELEVE = 1L;
    private static final long UTIL_ELEVE = 11L;
    private static final long PARENT = 12L;
    private static final long ETRANGER = 99L;

    private final DevoirFaitRepository faits = mock(DevoirFaitRepository.class);
    private final DevoirRepository devoirs = mock(DevoirRepository.class);
    private final DevoirService devoirService = mock(DevoirService.class);
    private final EleveRepository eleves = mock(EleveRepository.class);
    private final LienParentalRepository liens = mock(LienParentalRepository.class);
    private final InscriptionRepository inscriptions = mock(InscriptionRepository.class);
    private final PerimetreService perimetre = mock(PerimetreService.class);
    private final Clock horloge = Clock.fixed(Instant.parse("2026-10-30T10:00:00Z"), ZoneOffset.UTC);
    private final DevoirFaitService service = new DevoirFaitService(faits, devoirs, devoirService, eleves, liens,
            inscriptions, perimetre, horloge);

    private Eleve eleve;
    private Devoir devoir;

    private void connecteEn(long id) {
        UtilisateurPrincipal p = mock(UtilisateurPrincipal.class);
        when(p.getId()).thenReturn(id);
        when(perimetre.utilisateurCourant()).thenReturn(p);
    }

    @BeforeEach
    void preparer() {
        eleve = new Eleve();
        eleve.setId(ELEVE);
        when(eleves.findByUtilisateurId(UTIL_ELEVE)).thenReturn(Optional.of(eleve));
        when(eleves.findById(ELEVE)).thenReturn(Optional.of(eleve));
        when(liens.existsByEleveIdAndUtilisateurId(ELEVE, PARENT)).thenReturn(true);

        Classe classe = new Classe();
        classe.setId(CLASSE);
        Matiere matiere = new Matiere();
        matiere.setId(3L);
        matiere.setLibelle("Maths");
        Utilisateur u = new Utilisateur();
        u.setNom("Ba");
        u.setPrenom("Moussa");
        Enseignant ens = new Enseignant();
        ens.setUtilisateur(u);
        devoir = new Devoir();
        devoir.setId(DEVOIR);
        devoir.setClasse(classe);
        devoir.setMatiere(matiere);
        devoir.setEnseignant(ens);
        devoir.setTitre("Exercices");
        devoir.setDateLimite(LocalDate.of(2026, 11, 3));
        devoir.setCreatedAt(Instant.parse("2026-10-29T10:00:00Z"));
        when(devoirs.findById(DEVOIR)).thenReturn(Optional.of(devoir));
        when(inscriptions.existsByClasseIdAndEleveIdAndStatut(CLASSE, ELEVE, "ACCEPTEE")).thenReturn(true);
        when(faits.findByDevoirIdAndEleveId(DEVOIR, ELEVE)).thenReturn(Optional.empty());
    }

    @Test
    void unEleveCocheSonPropreDevoir() {
        connecteEn(UTIL_ELEVE);
        DevoirDto dto = service.marquer(DEVOIR, null, true);

        assertThat(dto.fait()).isTrue();
        org.mockito.ArgumentCaptor<DevoirFait> capture = org.mockito.ArgumentCaptor.forClass(DevoirFait.class);
        verify(faits).save(capture.capture());
        assertThat(capture.getValue().getParUtilisateurId()).isEqualTo(UTIL_ELEVE);
        assertThat(capture.getValue().getFaitLe()).isEqualTo(Instant.parse("2026-10-30T10:00:00Z"));
    }

    @Test
    void unParentCochePourSonEnfant() {
        connecteEn(PARENT);
        service.marquer(DEVOIR, ELEVE, true);
        verify(faits).save(any(DevoirFait.class));
    }

    @Test
    void cocherDeuxFoisNeCreePasDeDoublon() {
        connecteEn(UTIL_ELEVE);
        when(faits.findByDevoirIdAndEleveId(DEVOIR, ELEVE)).thenReturn(Optional.of(new DevoirFait()));
        service.marquer(DEVOIR, null, true);
        verify(faits, never()).save(any());
    }

    @Test
    void decocherSupprimeLaLigne() {
        connecteEn(UTIL_ELEVE);
        DevoirFait existant = new DevoirFait();
        when(faits.findByDevoirIdAndEleveId(DEVOIR, ELEVE)).thenReturn(Optional.of(existant));

        assertThat(service.marquer(DEVOIR, null, false).fait()).isFalse();
        verify(faits).delete(existant);
    }

    @Test
    void unEtrangerNePeutPasCocherPourUnAutreEleve() {
        connecteEn(ETRANGER);
        assertThatThrownBy(() -> service.marquer(DEVOIR, ELEVE, true)).isInstanceOf(AccessDeniedException.class);
        verify(faits, never()).save(any());
    }

    @Test
    void unEleveNeCochePasPourUnCamarade() {
        connecteEn(UTIL_ELEVE);
        assertThatThrownBy(() -> service.marquer(DEVOIR, 2L, true)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void lEleveDoitEtreDansLaClasseDuDevoir() {
        connecteEn(UTIL_ELEVE);
        when(inscriptions.existsByClasseIdAndEleveIdAndStatut(CLASSE, ELEVE, "ACCEPTEE")).thenReturn(false);
        assertThatThrownBy(() -> service.marquer(DEVOIR, null, true)).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void laListeDeLEleveIndiqueLesDevoirsTermines() {
        Inscription inscription = new Inscription();
        inscription.setClasse(devoir.getClasse());
        when(inscriptions.findByEleveIdAndStatutOrderByAnneeScolaireDateDebutDesc(ELEVE, "ACCEPTEE"))
                .thenReturn(List.of(inscription));
        DevoirDto d1 = DevoirDto.de(devoir);
        Devoir autre = new Devoir();
        autre.setId(10L);
        autre.setClasse(devoir.getClasse());
        autre.setMatiere(devoir.getMatiere());
        autre.setEnseignant(devoir.getEnseignant());
        autre.setTitre("Lecture");
        autre.setDateLimite(LocalDate.of(2026, 11, 4));
        DevoirDto d2 = DevoirDto.de(autre);
        when(devoirService.lister(CLASSE, null)).thenReturn(List.of(d1, d2));
        when(faits.idsTermines(ELEVE, List.of(DEVOIR, 10L))).thenReturn(List.of(DEVOIR));

        List<DevoirDto> liste = service.deLEleve(ELEVE);

        assertThat(liste).extracting(DevoirDto::fait).containsExactly(true, false);
    }

    @Test
    void sansClasseLaListeEstVide() {
        when(inscriptions.findByEleveIdAndStatutOrderByAnneeScolaireDateDebutDesc(ELEVE, "ACCEPTEE"))
                .thenReturn(List.of());
        assertThat(service.deLEleve(ELEVE)).isEmpty();
    }

    @Test
    void lEquipeVoitLaProgressionLesFamillesNon() {
        DevoirDto d = DevoirDto.de(devoir);
        when(faits.compter(List.of(DEVOIR))).thenReturn(List.<Object[]>of(new Object[] {DEVOIR, 12L}));
        when(inscriptions.countByClasseIdAndStatut(CLASSE, "ACCEPTEE")).thenReturn(28L);

        when(perimetre.peutConsulterPresences(CLASSE)).thenReturn(true);
        DevoirDto vuParLEquipe = service.avecProgres(CLASSE, List.of(d)).get(0);
        assertThat(vuParLEquipe.faits()).isEqualTo(12);
        assertThat(vuParLEquipe.effectif()).isEqualTo(28);

        when(perimetre.peutConsulterPresences(CLASSE)).thenReturn(false);
        DevoirDto vuParUneFamille = service.avecProgres(CLASSE, List.of(d)).get(0);
        assertThat(vuParUneFamille.faits()).isNull();
    }
}
