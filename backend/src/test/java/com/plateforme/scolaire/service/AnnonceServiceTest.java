package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.plateforme.scolaire.domain.Annonce;
import com.plateforme.scolaire.domain.Classe;
import com.plateforme.scolaire.domain.Utilisateur;
import com.plateforme.scolaire.repository.AnnonceRepository;
import com.plateforme.scolaire.repository.ClasseRepository;
import com.plateforme.scolaire.repository.UtilisateurRepository;
import com.plateforme.scolaire.security.PerimetreService;
import com.plateforme.scolaire.security.UtilisateurPrincipal;
import com.plateforme.scolaire.web.dto.AnnonceDto;
import com.plateforme.scolaire.web.dto.AnnonceRequest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

class AnnonceServiceTest {

    private static final long CLASSE = 5L;
    private static final long AUTEUR = 20L;
    private static final long AUTRE = 21L;

    private final AnnonceRepository annonces = mock(AnnonceRepository.class);
    private final ClasseRepository classes = mock(ClasseRepository.class);
    private final UtilisateurRepository utilisateurs = mock(UtilisateurRepository.class);
    private final PerimetreService perimetre = mock(PerimetreService.class);
    private final Clock horloge = Clock.fixed(Instant.parse("2026-10-30T10:00:00Z"), ZoneOffset.UTC);
    private final ApplicationEventPublisher evenements = mock(ApplicationEventPublisher.class);
    private final AnnonceService service =
            new AnnonceService(annonces, classes, utilisateurs, perimetre, horloge, evenements);

    private Classe classe;
    private Utilisateur auteur;
    private Annonce annonce;

    private void connecteEn(long id) {
        UtilisateurPrincipal p = mock(UtilisateurPrincipal.class);
        when(p.getId()).thenReturn(id);
        when(perimetre.utilisateurCourant()).thenReturn(p);
    }

    @BeforeEach
    void preparer() {
        classe = new Classe();
        classe.setId(CLASSE);
        auteur = new Utilisateur();
        auteur.setId(AUTEUR);
        auteur.setPrenom("Moussa");
        auteur.setNom("Ba");
        annonce = new Annonce();
        annonce.setId(3L);
        annonce.setClasse(classe);
        annonce.setAuteur(auteur);
        annonce.setTitre("Sortie");
        annonce.setContenu("Vendredi");
        annonce.setCreatedAt(Instant.parse("2026-10-29T10:00:00Z"));
        when(classes.existsById(CLASSE)).thenReturn(true);
        when(classes.findById(CLASSE)).thenReturn(Optional.of(classe));
        when(utilisateurs.getReferenceById(AUTEUR)).thenReturn(auteur);
        when(annonces.save(any(Annonce.class))).thenAnswer(i -> i.getArgument(0));
        when(annonces.recentes(org.mockito.ArgumentMatchers.eq(CLASSE), any(Pageable.class))).thenReturn(List.of(annonce));
        when(annonces.findById(3L)).thenReturn(Optional.of(annonce));
    }

    @Test
    void laPublicationEstDateeEtNettoyee() {
        connecteEn(AUTEUR);
        AnnonceDto dto = service.publier(CLASSE, new AnnonceRequest("  Réunion  ", "  Lundi à 18h  "));

        assertThat(dto.titre()).isEqualTo("Réunion");
        assertThat(dto.contenu()).isEqualTo("Lundi à 18h");
        assertThat(dto.auteur()).isEqualTo("Moussa Ba");
        assertThat(dto.publieLe()).isEqualTo(Instant.parse("2026-10-30T10:00:00Z"));        verify(evenements).publishEvent(new AnnoncePubliee(CLASSE, null, "Réunion", "Moussa Ba"));
    }

    @Test
    void seulLAuteurOuLaDirectionPeutSupprimerDansLaListe() {
        connecteEn(AUTEUR);
        assertThat(service.lister(CLASSE).get(0).modifiable()).isTrue();

        connecteEn(AUTRE);
        assertThat(service.lister(CLASSE).get(0).modifiable()).isFalse();

        when(perimetre.peutGererClasse(CLASSE)).thenReturn(true);
        assertThat(service.lister(CLASSE).get(0).modifiable()).isTrue();
    }

    @Test
    void lAuteurSupprimeSonAnnonce() {
        connecteEn(AUTEUR);
        service.supprimer(3L);
        verify(annonces).delete(annonce);
    }

    @Test
    void laDirectionSupprimeLAnnonceDUnEnseignant() {
        connecteEn(AUTRE);
        when(perimetre.peutGererClasse(CLASSE)).thenReturn(true);
        service.supprimer(3L);
        verify(annonces).delete(annonce);
    }

    @Test
    void unAutreEnseignantNePeutPasSupprimer() {
        connecteEn(AUTRE);
        assertThatThrownBy(() -> service.supprimer(3L)).isInstanceOf(AccessDeniedException.class);
        verify(annonces, never()).delete(any());
    }

    @Test
    void uneClasseInconnueDonne404() {
        connecteEn(AUTEUR);
        when(classes.existsById(99L)).thenReturn(false);
        assertThatThrownBy(() -> service.lister(99L)).isInstanceOf(ResponseStatusException.class);
    }
}
