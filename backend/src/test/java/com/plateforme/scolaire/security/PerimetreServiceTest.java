package com.plateforme.scolaire.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.plateforme.scolaire.domain.Etablissement;
import com.plateforme.scolaire.domain.Perimetre;
import com.plateforme.scolaire.domain.Role;
import com.plateforme.scolaire.domain.ZoneGeographique;
import com.plateforme.scolaire.repository.ClasseRepository;
import com.plateforme.scolaire.repository.EtablissementRepository;
import com.plateforme.scolaire.security.UtilisateurPrincipal.AffectationInfo;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class PerimetreServiceTest {

    private final EtablissementRepository etablissementRepository = mock(EtablissementRepository.class);
    private final ClasseRepository classeRepository = mock(ClasseRepository.class);
    private final PerimetreService service = new PerimetreService(etablissementRepository, classeRepository,
            mock(com.plateforme.scolaire.repository.InscriptionRepository.class), mock(com.plateforme.scolaire.repository.EleveRepository.class),
            mock(com.plateforme.scolaire.repository.LienParentalRepository.class),
            mock(com.plateforme.scolaire.repository.AffectationEnseignantRepository.class));

    @AfterEach
    void nettoyer() {
        SecurityContextHolder.clearContext();
    }

    private void connecter(AffectationInfo... affectations) {
        UtilisateurPrincipal p = new UtilisateurPrincipal(1L, "u@example.org", "x", true, List.of(affectations));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(p, null, p.getAuthorities()));
    }

    private static AffectationInfo direction(Long etablissementId) {
        return new AffectationInfo(Role.DIRECTION_ETABLISSEMENT, Perimetre.ETABLISSEMENT, null, etablissementId);
    }

    private static ZoneGeographique zone(Long id, ZoneGeographique parent) {
        ZoneGeographique z = new ZoneGeographique();
        z.setId(id);
        z.setParent(parent);
        return z;
    }

    @Test
    void laDirectionAccedeASonEtablissementMaisPasAUnAutre() {
        connecter(direction(1L));
        assertThat(service.peutAccederEtablissement(1L)).isTrue();
        assertThat(service.peutAccederEtablissement(2L)).isFalse();
    }

    @Test
    void laDirectionNeGerePasUnAutreEtablissement() {
        connecter(direction(1L));
        assertThat(service.peutGererEtablissement(1L)).isTrue();
        assertThat(service.peutGererEtablissement(2L)).isFalse();
    }

    @Test
    void laDirectionAccedeAuxClassesDeSonEtablissementSeulement() {
        when(classeRepository.findEtablissementIdById(5L)).thenReturn(Optional.of(1L));
        when(classeRepository.findEtablissementIdById(6L)).thenReturn(Optional.of(2L));
        connecter(direction(1L));
        assertThat(service.peutAccederClasse(5L)).isTrue();
        assertThat(service.peutGererClasse(5L)).isTrue();
        assertThat(service.peutAccederClasse(6L)).isFalse();
        assertThat(service.peutGererClasse(6L)).isFalse();
    }

    @Test
    void uneClasseInexistanteEstRefusee() {
        connecter(direction(1L));
        assertThat(service.peutAccederClasse(99L)).isFalse();
        assertThat(service.peutGererClasse(99L)).isFalse();
    }

    @Test
    void leMinistereVoitToutMaisNeGerePasLesEtablissements() {
        connecter(new AffectationInfo(Role.MINISTERE, Perimetre.NATIONAL, null, null));
        assertThat(service.peutAccederEtablissement(1L)).isTrue();
        assertThat(service.peutAccederEtablissement(2L)).isTrue();
        assertThat(service.peutGererEtablissement(1L)).isFalse();
    }

    @Test
    void l_administrateurTechniqueNAAucunAccesAuxDonneesDesEtablissements() {
        connecter(new AffectationInfo(Role.ADMIN_TECHNIQUE, Perimetre.NATIONAL, null, null));
        assertThat(service.peutAccederEtablissement(1L)).isFalse();
        assertThat(service.peutGererEtablissement(1L)).isFalse();
    }

    @Test
    void unEnseignantLitSonEtablissementMaisNeLeGerePas() {
        connecter(new AffectationInfo(Role.ENSEIGNANT, Perimetre.ETABLISSEMENT, null, 1L));
        assertThat(service.peutAccederEtablissement(1L)).isTrue();
        assertThat(service.peutGererEtablissement(1L)).isFalse();
        assertThat(service.peutAccederEtablissement(2L)).isFalse();
    }

    @Test
    void unParentNAPasAccesAUnEtablissementEnTantQueTel() {
        connecter(new AffectationInfo(Role.PARENT, Perimetre.PERSONNEL, null, null));
        assertThat(service.peutAccederEtablissement(1L)).isFalse();
    }

    @Test
    void uneDirectionRegionaleAccedeAuxEtablissementsDeSesSousZones() {
        ZoneGeographique pays = zone(1L, null);
        ZoneGeographique region = zone(2L, pays);
        ZoneGeographique departement = zone(3L, region);
        Etablissement e = new Etablissement();
        e.setId(10L);
        e.setZone(departement);
        when(etablissementRepository.findById(10L)).thenReturn(Optional.of(e));

        connecter(new AffectationInfo(Role.DIRECTION_REGIONALE, Perimetre.ZONE, 2L, null));
        assertThat(service.peutAccederEtablissement(10L)).isTrue();
        assertThat(service.peutGererEtablissement(10L)).isFalse();

        connecter(new AffectationInfo(Role.DIRECTION_REGIONALE, Perimetre.ZONE, 9L, null));
        assertThat(service.peutAccederEtablissement(10L)).isFalse();
    }

    @Test
    void sansAuthentificationTousLesAccesSontRefuses() {
        assertThat(service.peutAccederEtablissement(1L)).isFalse();
        assertThat(service.peutGererEtablissement(1L)).isFalse();
        assertThat(service.peutAccederClasse(1L)).isFalse();
    }
}
