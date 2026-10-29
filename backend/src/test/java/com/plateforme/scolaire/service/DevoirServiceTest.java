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
import com.plateforme.scolaire.domain.Enseignant;
import com.plateforme.scolaire.domain.Matiere;
import com.plateforme.scolaire.domain.Utilisateur;
import com.plateforme.scolaire.repository.ClasseRepository;
import com.plateforme.scolaire.repository.DevoirRepository;
import com.plateforme.scolaire.repository.EnseignantRepository;
import com.plateforme.scolaire.repository.MatiereRepository;
import com.plateforme.scolaire.security.PerimetreNotesService;
import com.plateforme.scolaire.security.PerimetreService;
import com.plateforme.scolaire.security.UtilisateurPrincipal;
import com.plateforme.scolaire.web.dto.DevoirDto;
import com.plateforme.scolaire.web.dto.DevoirRequest;
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

class DevoirServiceTest {

    private static final long CLASSE = 5L;
    private static final long MATIERE = 7L;

    private final DevoirRepository devoirRepository = mock(DevoirRepository.class);
    private final ClasseRepository classeRepository = mock(ClasseRepository.class);
    private final MatiereRepository matiereRepository = mock(MatiereRepository.class);
    private final EnseignantRepository enseignantRepository = mock(EnseignantRepository.class);
    private final PerimetreService perimetreService = mock(PerimetreService.class);
    private final PerimetreNotesService perimetreNotes = mock(PerimetreNotesService.class);
    private final Clock horloge = Clock.fixed(Instant.parse("2026-10-29T10:00:00Z"), ZoneOffset.UTC);
    private final DevoirService service = new DevoirService(devoirRepository, classeRepository, matiereRepository,
            enseignantRepository, perimetreService, perimetreNotes, horloge);

    private Classe classe;
    private Matiere matiere;
    private Enseignant enseignant;

    @BeforeEach
    void preparer() {
        classe = new Classe();
        classe.setId(CLASSE);
        matiere = new Matiere();
        matiere.setId(MATIERE);
        matiere.setLibelle("Mathématiques");
        Utilisateur u = new Utilisateur();
        u.setId(40L);
        u.setNom("Diallo");
        u.setPrenom("Awa");
        enseignant = new Enseignant();
        enseignant.setId(3L);
        enseignant.setUtilisateur(u);

        UtilisateurPrincipal principal = mock(UtilisateurPrincipal.class);
        when(principal.getId()).thenReturn(40L);
        when(perimetreService.utilisateurCourant()).thenReturn(principal);
        when(classeRepository.existsById(CLASSE)).thenReturn(true);
        when(classeRepository.findById(CLASSE)).thenReturn(Optional.of(classe));
        when(matiereRepository.findById(MATIERE)).thenReturn(Optional.of(matiere));
        when(enseignantRepository.findByUtilisateurId(40L)).thenReturn(Optional.of(enseignant));
        when(perimetreNotes.peutEvaluerMatiere(CLASSE, MATIERE)).thenReturn(true);
        when(devoirRepository.save(any(Devoir.class))).thenAnswer(i -> i.getArgument(0));
    }

    private static DevoirRequest requete(LocalDate date) {
        return new DevoirRequest(MATIERE, "  Exercices 1 à 5  ", "  Page 42  ", date);
    }

    @Test
    void unEnseignantPublieUnDevoirPourSaMatiere() {
        DevoirDto dto = service.creer(CLASSE, requete(LocalDate.of(2026, 11, 3)));

        assertThat(dto.titre()).isEqualTo("Exercices 1 à 5");
        assertThat(dto.consigne()).isEqualTo("Page 42");
        assertThat(dto.matiere()).isEqualTo("Mathématiques");
        assertThat(dto.enseignant()).isEqualTo("Awa Diallo");
        assertThat(dto.dateLimite()).isEqualTo(LocalDate.of(2026, 11, 3));
        assertThat(dto.creeLe()).isEqualTo(Instant.parse("2026-10-29T10:00:00Z"));
    }

    @Test
    void uneConsigneVideDevientAbsente() {
        DevoirDto dto = service.creer(CLASSE, new DevoirRequest(MATIERE, "Lecture", "   ", LocalDate.of(2026, 11, 3)));
        assertThat(dto.consigne()).isNull();
    }

    @Test
    void ladateDuJourEstAcceptee() {
        assertThat(service.creer(CLASSE, requete(LocalDate.of(2026, 10, 29))).dateLimite())
                .isEqualTo(LocalDate.of(2026, 10, 29));
    }

    @Test
    void uneDateLimiteDansLePasseEstRefusee() {
        assertThatThrownBy(() -> service.creer(CLASSE, requete(LocalDate.of(2026, 10, 28))))
                .isInstanceOf(ResponseStatusException.class);
        verify(devoirRepository, never()).save(any());
    }

    @Test
    void unEnseignantNeSaisitPasUneMatiereQuIlNEnseignePasDansLaClasse() {
        when(perimetreNotes.peutEvaluerMatiere(CLASSE, MATIERE)).thenReturn(false);
        assertThatThrownBy(() -> service.creer(CLASSE, requete(LocalDate.of(2026, 11, 3))))
                .isInstanceOf(AccessDeniedException.class);
        verify(devoirRepository, never()).save(any());
    }

    @Test
    void laMatiereEstObligatoireALaCreation() {
        assertThatThrownBy(() -> service.creer(CLASSE,
                new DevoirRequest(null, "Lecture", null, LocalDate.of(2026, 11, 3))))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void parDefautLaListeRemonteDe14Jours() {
        when(devoirRepository.findByClasseIdAndDateLimiteGreaterThanEqualOrderByDateLimiteAscIdAsc(CLASSE,
                LocalDate.of(2026, 10, 15))).thenReturn(List.of());
        assertThat(service.lister(CLASSE, null)).isEmpty();
        verify(devoirRepository).findByClasseIdAndDateLimiteGreaterThanEqualOrderByDateLimiteAscIdAsc(CLASSE,
                LocalDate.of(2026, 10, 15));
    }

    @Test
    void lamodificationChangeTitreConsigneEtDate() {
        Devoir existant = new Devoir();
        existant.setId(9L);
        existant.setClasse(classe);
        existant.setMatiere(matiere);
        existant.setEnseignant(enseignant);
        existant.setTitre("Ancien");
        existant.setDateLimite(LocalDate.of(2026, 11, 1));
        existant.setCreatedAt(Instant.parse("2026-10-20T10:00:00Z"));
        when(devoirRepository.findById(9L)).thenReturn(Optional.of(existant));

        DevoirDto dto = service.modifier(9L, new DevoirRequest(null, "Nouveau", "Détails", LocalDate.of(2026, 11, 5)));

        assertThat(dto.titre()).isEqualTo("Nouveau");
        assertThat(dto.consigne()).isEqualTo("Détails");
        assertThat(dto.dateLimite()).isEqualTo(LocalDate.of(2026, 11, 5));
        assertThat(dto.matiereId()).isEqualTo(MATIERE);
    }
}
