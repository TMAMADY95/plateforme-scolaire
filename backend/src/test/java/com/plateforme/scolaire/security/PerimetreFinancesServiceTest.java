package com.plateforme.scolaire.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.plateforme.scolaire.domain.Perimetre;
import com.plateforme.scolaire.domain.Role;
import com.plateforme.scolaire.repository.FactureRepository;
import com.plateforme.scolaire.repository.FraisRepository;
import com.plateforme.scolaire.security.UtilisateurPrincipal.AffectationInfo;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Les factures nomment des élèves et révèlent des bourses : seule la direction de l'établissement
 * les consulte. Le bilan (sans nom) est aussi lisible par le ministère et les directions régionales.
 */
class PerimetreFinancesServiceTest {

    private static final long ETABLISSEMENT = 4L;
    private static final long FACTURE = 9L;

    private final PerimetreService perimetreService = mock(PerimetreService.class);
    private final FactureRepository factureRepository = mock(FactureRepository.class);
    private final PerimetreFinancesService service =
            new PerimetreFinancesService(perimetreService, mock(FraisRepository.class), factureRepository);

    private void connecter(AffectationInfo affectation) {
        when(perimetreService.utilisateurCourant()).thenReturn(
                new UtilisateurPrincipal(1L, "u@example.org", "x", true, List.of(affectation)));
    }

    @Test
    void laDirectionConsulteLesFacturesEtLeBilan() {
        connecter(new AffectationInfo(Role.DIRECTION_ETABLISSEMENT, Perimetre.ETABLISSEMENT, null, ETABLISSEMENT));
        when(perimetreService.peutGererEtablissement(ETABLISSEMENT)).thenReturn(true);

        assertThat(service.peutConsulterFactures(ETABLISSEMENT)).isTrue();
        assertThat(service.peutConsulterBilan(ETABLISSEMENT)).isTrue();
    }

    @Test
    void unEnseignantDeLEtablissementNeVoitNiLesFacturesNiLeBilan() {
        connecter(new AffectationInfo(Role.ENSEIGNANT, Perimetre.ETABLISSEMENT, null, ETABLISSEMENT));
        when(perimetreService.peutAccederEtablissement(ETABLISSEMENT)).thenReturn(true);
        when(perimetreService.peutGererEtablissement(ETABLISSEMENT)).thenReturn(false);

        assertThat(service.peutConsulterFactures(ETABLISSEMENT)).isFalse();
        assertThat(service.peutConsulterBilan(ETABLISSEMENT)).isFalse();
    }

    @Test
    void leMinistereVoitLeBilanMaisPasLesFacturesNominatives() {
        connecter(new AffectationInfo(Role.MINISTERE, Perimetre.NATIONAL, null, null));
        when(perimetreService.peutAccederEtablissement(ETABLISSEMENT)).thenReturn(true);
        when(perimetreService.peutGererEtablissement(ETABLISSEMENT)).thenReturn(false);

        assertThat(service.peutConsulterBilan(ETABLISSEMENT)).isTrue();
        assertThat(service.peutConsulterFactures(ETABLISSEMENT)).isFalse();
    }

    @Test
    void lAccesAUneFactureSuitLaRegleDesFactures() {
        connecter(new AffectationInfo(Role.ENSEIGNANT, Perimetre.ETABLISSEMENT, null, ETABLISSEMENT));
        when(factureRepository.findEtablissementIdById(FACTURE)).thenReturn(Optional.of(ETABLISSEMENT));
        when(perimetreService.peutAccederEtablissement(ETABLISSEMENT)).thenReturn(true);
        when(perimetreService.peutGererEtablissement(ETABLISSEMENT)).thenReturn(false);

        assertThat(service.peutAccederFacture(FACTURE)).isFalse();
        assertThat(service.peutAccederFacture(null)).isFalse();
    }
}
