package com.plateforme.scolaire.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.plateforme.scolaire.domain.Perimetre;
import com.plateforme.scolaire.domain.Role;
import com.plateforme.scolaire.repository.ClasseRepository;
import com.plateforme.scolaire.repository.EleveRepository;
import com.plateforme.scolaire.repository.EtablissementRepository;
import com.plateforme.scolaire.repository.InscriptionRepository;
import com.plateforme.scolaire.repository.LienParentalRepository;
import com.plateforme.scolaire.security.UtilisateurPrincipal.AffectationInfo;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/** Accès aux élèves et aux inscriptions : cloisonnement par établissement, parents et élèves. */
class PerimetreEleveTest {

    private static final long ELEVE = 100L;
    private static final long UTILISATEUR = 1L;

    private final InscriptionRepository inscriptionRepository = mock(InscriptionRepository.class);
    private final EleveRepository eleveRepository = mock(EleveRepository.class);
    private final LienParentalRepository lienParentalRepository = mock(LienParentalRepository.class);
    private final PerimetreService service = new PerimetreService(
            mock(EtablissementRepository.class), mock(ClasseRepository.class),
            inscriptionRepository, eleveRepository, lienParentalRepository,
            mock(com.plateforme.scolaire.repository.AffectationEnseignantRepository.class));

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

    private void eleveInscritDans(Long... etablissements) {
        when(inscriptionRepository.findEtablissementIdsActifsByEleveId(ELEVE)).thenReturn(List.of(etablissements));
    }

    @Test
    void laDirectionVoitEtGereLesElevesDeSonEtablissementSeulement() {
        eleveInscritDans(1L);
        connecter(direction(1L));
        assertThat(service.peutAccederEleve(ELEVE)).isTrue();
        assertThat(service.peutGererEleve(ELEVE)).isTrue();

        connecter(direction(2L));
        assertThat(service.peutAccederEleve(ELEVE)).isFalse();
        assertThat(service.peutGererEleve(ELEVE)).isFalse();
    }

    @Test
    void unEleveSansInscriptionActiveEstInvisibleDesEtablissements() {
        eleveInscritDans();
        connecter(direction(1L));
        assertThat(service.peutAccederEleve(ELEVE)).isFalse();
    }

    @Test
    void leMinistereLitLesElevesMaisNeLesModifiePas() {
        eleveInscritDans(1L);
        connecter(new AffectationInfo(Role.MINISTERE, Perimetre.NATIONAL, null, null));
        assertThat(service.peutAccederEleve(ELEVE)).isTrue();
        assertThat(service.peutGererEleve(ELEVE)).isFalse();
    }

    @Test
    void unParentVoitSonEnfantMaisPasLesAutres() {
        eleveInscritDans();
        when(lienParentalRepository.existsByEleveIdAndUtilisateurId(ELEVE, UTILISATEUR)).thenReturn(true);
        connecter(new AffectationInfo(Role.PARENT, Perimetre.PERSONNEL, null, null));
        assertThat(service.peutAccederEleve(ELEVE)).isTrue();
        assertThat(service.peutGererEleve(ELEVE)).isFalse();
        assertThat(service.peutAccederEleve(ELEVE + 1)).isFalse();
    }

    @Test
    void unEleveVoitSonPropreDossier() {
        eleveInscritDans();
        when(eleveRepository.existsByIdAndUtilisateurId(ELEVE, UTILISATEUR)).thenReturn(true);
        connecter(new AffectationInfo(Role.ELEVE, Perimetre.PERSONNEL, null, null));
        assertThat(service.peutAccederEleve(ELEVE)).isTrue();
    }

    @Test
    void lAdministrateurTechniqueNAPasAccesAuxEleves() {
        eleveInscritDans(1L);
        connecter(new AffectationInfo(Role.ADMIN_TECHNIQUE, Perimetre.NATIONAL, null, null));
        assertThat(service.peutAccederEleve(ELEVE)).isFalse();
    }

    @Test
    void lesInscriptionsSontCloisonneesParEtablissement() {
        when(inscriptionRepository.findEtablissementIdById(7L)).thenReturn(Optional.of(1L));
        connecter(direction(1L));
        assertThat(service.peutAccederInscription(7L)).isTrue();
        assertThat(service.peutGererInscription(7L)).isTrue();

        connecter(direction(2L));
        assertThat(service.peutAccederInscription(7L)).isFalse();
        assertThat(service.peutGererInscription(7L)).isFalse();
    }

    @Test
    void uneInscriptionInexistanteEstRefusee() {
        connecter(direction(1L));
        assertThat(service.peutAccederInscription(99L)).isFalse();
        assertThat(service.peutGererInscription(99L)).isFalse();
    }

    @Test
    void sansAuthentificationTousLesAccesElevesSontRefuses() {
        eleveInscritDans(1L);
        assertThat(service.peutAccederEleve(ELEVE)).isFalse();
        assertThat(service.peutGererEleve(ELEVE)).isFalse();
    }
}
