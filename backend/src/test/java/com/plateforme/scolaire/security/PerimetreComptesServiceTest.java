package com.plateforme.scolaire.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.plateforme.scolaire.domain.AffectationRole;
import com.plateforme.scolaire.domain.Etablissement;
import com.plateforme.scolaire.domain.Perimetre;
import com.plateforme.scolaire.domain.Role;
import com.plateforme.scolaire.repository.AffectationRoleRepository;
import com.plateforme.scolaire.repository.EleveRepository;
import com.plateforme.scolaire.repository.InscriptionRepository;
import com.plateforme.scolaire.repository.LienParentalRepository;
import com.plateforme.scolaire.security.UtilisateurPrincipal.AffectationInfo;
import java.util.List;
import org.junit.jupiter.api.Test;

class PerimetreComptesServiceTest {

    private static final Long MOI = 1L;
    private static final Long CIBLE = 50L;

    private final PerimetreService perimetreService = mock(PerimetreService.class);
    private final AffectationRoleRepository affectationRoleRepository = mock(AffectationRoleRepository.class);
    private final LienParentalRepository lienParentalRepository = mock(LienParentalRepository.class);
    private final EleveRepository eleveRepository = mock(EleveRepository.class);
    private final InscriptionRepository inscriptionRepository = mock(InscriptionRepository.class);

    private final PerimetreComptesService service = new PerimetreComptesService(perimetreService,
            affectationRoleRepository, lienParentalRepository, eleveRepository, inscriptionRepository);

    private void connecter(AffectationInfo... affectations) {
        when(perimetreService.utilisateurCourant())
                .thenReturn(new UtilisateurPrincipal(MOI, "moi@example.org", "x", true, List.of(affectations)));
    }

    private static AffectationInfo direction(Long etablissementId) {
        return new AffectationInfo(Role.DIRECTION_ETABLISSEMENT, Perimetre.ETABLISSEMENT, null, etablissementId);
    }

    private static AffectationInfo ministere() {
        return new AffectationInfo(Role.MINISTERE, Perimetre.NATIONAL, null, null);
    }

    private static AffectationRole role(Role r, Perimetre perimetre, Long etablissementId) {
        AffectationRole a = new AffectationRole();
        a.setRole(r);
        a.setPerimetre(perimetre);
        if (etablissementId != null) {
            Etablissement e = new Etablissement();
            e.setId(etablissementId);
            a.setEtablissement(e);
        }
        return a;
    }

    private void cibleAvec(AffectationRole... roles) {
        when(affectationRoleRepository.findByUtilisateurId(CIBLE)).thenReturn(List.of(roles));
    }

    @Test
    void leMinistereReinitialiseUneDirectionMaisPasUnEnseignant() {
        connecter(ministere());

        cibleAvec(role(Role.DIRECTION_ETABLISSEMENT, Perimetre.ETABLISSEMENT, 7L));
        assertThat(service.peutReinitialiser(CIBLE)).isTrue();

        cibleAvec(role(Role.ENSEIGNANT, Perimetre.ETABLISSEMENT, 7L));
        assertThat(service.peutReinitialiser(CIBLE)).isFalse();
    }

    @Test
    void laDirectionReinitialiseLesEnseignantsEtLePersonnelDeSonEtablissementSeulement() {
        connecter(direction(7L));

        cibleAvec(role(Role.ENSEIGNANT, Perimetre.ETABLISSEMENT, 7L));
        assertThat(service.peutReinitialiser(CIBLE)).isTrue();

        cibleAvec(role(Role.PERSONNEL_ADMIN, Perimetre.ETABLISSEMENT, 7L));
        assertThat(service.peutReinitialiser(CIBLE)).isTrue();

        cibleAvec(role(Role.ENSEIGNANT, Perimetre.ETABLISSEMENT, 8L));
        assertThat(service.peutReinitialiser(CIBLE)).isFalse();
    }

    @Test
    void laDirectionNeToucheNiAuMinistereNiAUneAutreDirection() {
        connecter(direction(7L));

        cibleAvec(role(Role.MINISTERE, Perimetre.NATIONAL, null));
        assertThat(service.peutReinitialiser(CIBLE)).isFalse();

        cibleAvec(role(Role.DIRECTION_ETABLISSEMENT, Perimetre.ETABLISSEMENT, 7L));
        assertThat(service.peutReinitialiser(CIBLE)).isFalse();
    }

    @Test
    void uneCibleQuiCumuleUnRoleHorsPerimetreEstRefusee() {
        connecter(direction(7L));
        cibleAvec(role(Role.ENSEIGNANT, Perimetre.ETABLISSEMENT, 7L),
                role(Role.ENSEIGNANT, Perimetre.ETABLISSEMENT, 8L));

        assertThat(service.peutReinitialiser(CIBLE)).isFalse();
    }

    @Test
    void personneNeReinitialiseSonPropreMotDePasseParCetteVoie() {
        connecter(ministere());
        when(affectationRoleRepository.findByUtilisateurId(MOI))
                .thenReturn(List.of(role(Role.DIRECTION_REGIONALE, Perimetre.ZONE, null)));

        assertThat(service.peutReinitialiser(MOI)).isFalse();
    }

    @Test
    void unCompteSansRoleOuInconnuEstRefuse() {
        connecter(ministere());
        when(affectationRoleRepository.findByUtilisateurId(CIBLE)).thenReturn(List.of());

        assertThat(service.peutReinitialiser(CIBLE)).isFalse();
    }

    @Test
    void laDirectionReinitialiseUnParentDontUnEnfantEstInscritChezElle() {
        connecter(direction(7L));
        cibleAvec(role(Role.PARENT, Perimetre.PERSONNEL, null));
        when(lienParentalRepository.findEleveIdsByUtilisateurId(CIBLE)).thenReturn(List.of(100L, 101L));
        when(inscriptionRepository.findEtablissementIdsActifsByEleveId(100L)).thenReturn(List.of(9L));
        when(inscriptionRepository.findEtablissementIdsActifsByEleveId(101L)).thenReturn(List.of(7L));

        assertThat(service.peutReinitialiser(CIBLE)).isTrue();
    }

    @Test
    void laDirectionNeReinitialiseUnParentSansEnfantChezElle() {
        connecter(direction(7L));
        cibleAvec(role(Role.PARENT, Perimetre.PERSONNEL, null));
        when(lienParentalRepository.findEleveIdsByUtilisateurId(CIBLE)).thenReturn(List.of(100L));
        when(inscriptionRepository.findEtablissementIdsActifsByEleveId(100L)).thenReturn(List.of(9L));

        assertThat(service.peutReinitialiser(CIBLE)).isFalse();
    }

    @Test
    void seuleLaDirectionDeLEtablissementCreeLePersonnel() {
        connecter(direction(7L));
        assertThat(service.estDirectionDe(7L)).isTrue();
        assertThat(service.estDirectionDe(8L)).isFalse();

        connecter(new AffectationInfo(Role.PERSONNEL_ADMIN, Perimetre.ETABLISSEMENT, null, 7L));
        assertThat(service.estDirectionDe(7L)).isFalse();
        assertThat(service.peutListerComptes(7L)).isFalse();
    }
}
