package com.plateforme.scolaire.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.plateforme.scolaire.domain.Etablissement;
import com.plateforme.scolaire.domain.Perimetre;
import com.plateforme.scolaire.domain.Role;
import com.plateforme.scolaire.domain.ZoneGeographique;
import com.plateforme.scolaire.repository.AffectationEnseignantRepository;
import com.plateforme.scolaire.repository.ClasseRepository;
import com.plateforme.scolaire.repository.EleveRepository;
import com.plateforme.scolaire.repository.EtablissementRepository;
import com.plateforme.scolaire.repository.InscriptionRepository;
import com.plateforme.scolaire.repository.LienParentalRepository;
import com.plateforme.scolaire.security.UtilisateurPrincipal.AffectationInfo;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/** Qui peut saisir et consulter l'appel d'une classe ? Classes 5 et 6 : établissement 1. Classe 7 : établissement 2. */
class PerimetrePresenceTest {

    private static final long UTILISATEUR = 1L;

    private final EtablissementRepository etablissementRepository = mock(EtablissementRepository.class);
    private final ClasseRepository classeRepository = mock(ClasseRepository.class);
    private final AffectationEnseignantRepository affectationEnseignantRepository =
            mock(AffectationEnseignantRepository.class);
    private final PerimetreService service = new PerimetreService(
            etablissementRepository, classeRepository, mock(InscriptionRepository.class),
            mock(EleveRepository.class), mock(LienParentalRepository.class), affectationEnseignantRepository);

    @BeforeEach
    void preparer() {
        when(classeRepository.findEtablissementIdById(5L)).thenReturn(Optional.of(1L));
        when(classeRepository.findEtablissementIdById(6L)).thenReturn(Optional.of(1L));
        when(classeRepository.findEtablissementIdById(7L)).thenReturn(Optional.of(2L));
    }

    @AfterEach
    void nettoyer() {
        SecurityContextHolder.clearContext();
    }

    private void connecter(AffectationInfo... affectations) {
        UtilisateurPrincipal p = new UtilisateurPrincipal(UTILISATEUR, "u@example.org", "x", true,
                List.of(affectations));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(p, null, p.getAuthorities()));
    }

    private static AffectationInfo direction(Long etablissementId) {
        return new AffectationInfo(Role.DIRECTION_ETABLISSEMENT, Perimetre.ETABLISSEMENT, null, etablissementId);
    }

    private static AffectationInfo enseignant(Long etablissementId) {
        return new AffectationInfo(Role.ENSEIGNANT, Perimetre.ETABLISSEMENT, null, etablissementId);
    }

    @Test
    void laDirectionSaisitEtConsulteLesClassesDeSonEtablissementSeulement() {
        connecter(direction(1L));
        assertThat(service.peutSaisirPresences(5L)).isTrue();
        assertThat(service.peutConsulterPresences(5L)).isTrue();
        assertThat(service.peutSaisirPresences(7L)).isFalse();
        assertThat(service.peutConsulterPresences(7L)).isFalse();
    }

    @Test
    void unEnseignantNeGereQueSesClasses() {
        when(affectationEnseignantRepository.existsByClasseIdAndEnseignantUtilisateurId(5L, UTILISATEUR))
                .thenReturn(true);
        connecter(enseignant(1L));
        assertThat(service.peutSaisirPresences(5L)).isTrue();
        assertThat(service.peutConsulterPresences(5L)).isTrue();
        // Même établissement, mais pas sa classe
        assertThat(service.peutSaisirPresences(6L)).isFalse();
        assertThat(service.peutConsulterPresences(6L)).isFalse();
    }

    @Test
    void unEnseignantNAPasAccesAuxClassesDUnAutreEtablissement() {
        connecter(enseignant(1L));
        assertThat(service.peutSaisirPresences(7L)).isFalse();
        assertThat(service.peutConsulterPresences(7L)).isFalse();
    }

    @Test
    void leMinistereConsulteLesPresencesMaisNeLesSaisitPas() {
        connecter(new AffectationInfo(Role.MINISTERE, Perimetre.NATIONAL, null, null));
        assertThat(service.peutConsulterPresences(5L)).isTrue();
        assertThat(service.peutConsulterPresences(7L)).isTrue();
        assertThat(service.peutSaisirPresences(5L)).isFalse();
    }

    @Test
    void uneDirectionRegionaleConsulteLesClassesDeSaZone() {
        ZoneGeographique region = new ZoneGeographique();
        region.setId(2L);
        Etablissement ecole1 = new Etablissement();
        ecole1.setId(1L);
        ecole1.setZone(region);
        when(etablissementRepository.findById(1L)).thenReturn(Optional.of(ecole1));

        connecter(new AffectationInfo(Role.DIRECTION_REGIONALE, Perimetre.ZONE, 2L, null));
        assertThat(service.peutConsulterPresences(5L)).isTrue();
        assertThat(service.peutSaisirPresences(5L)).isFalse();

        connecter(new AffectationInfo(Role.DIRECTION_REGIONALE, Perimetre.ZONE, 9L, null));
        assertThat(service.peutConsulterPresences(5L)).isFalse();
    }

    @Test
    void lAdministrateurTechniqueEtLesParentsNAccedentPasAuxPresencesDesClasses() {
        connecter(new AffectationInfo(Role.ADMIN_TECHNIQUE, Perimetre.NATIONAL, null, null));
        assertThat(service.peutConsulterPresences(5L)).isFalse();
        assertThat(service.peutSaisirPresences(5L)).isFalse();

        connecter(new AffectationInfo(Role.PARENT, Perimetre.PERSONNEL, null, null));
        assertThat(service.peutConsulterPresences(5L)).isFalse();
        assertThat(service.peutSaisirPresences(5L)).isFalse();
    }

    @Test
    void uneClasseInexistanteOuUnUtilisateurAnonymeSontRefuses() {
        connecter(direction(1L));
        assertThat(service.peutSaisirPresences(99L)).isFalse();
        assertThat(service.peutConsulterPresences(99L)).isFalse();

        SecurityContextHolder.clearContext();
        assertThat(service.peutSaisirPresences(5L)).isFalse();
        assertThat(service.peutConsulterPresences(5L)).isFalse();
    }
}
