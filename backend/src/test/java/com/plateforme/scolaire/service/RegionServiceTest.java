package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.plateforme.scolaire.domain.Etablissement;
import com.plateforme.scolaire.domain.Perimetre;
import com.plateforme.scolaire.domain.Role;
import com.plateforme.scolaire.repository.ClasseRepository;
import com.plateforme.scolaire.repository.EtablissementRepository;
import com.plateforme.scolaire.repository.InscriptionRepository;
import com.plateforme.scolaire.security.PerimetreService;
import com.plateforme.scolaire.security.UtilisateurPrincipal;
import com.plateforme.scolaire.security.UtilisateurPrincipal.AffectationInfo;
import com.plateforme.scolaire.web.dto.SyntheseNationaleDto;
import java.util.List;
import org.junit.jupiter.api.Test;

class RegionServiceTest {

    private static final long ANNEE = 2L;

    private final EtablissementRepository etablissementRepository = mock(EtablissementRepository.class);
    private final ClasseRepository classeRepository = mock(ClasseRepository.class);
    private final InscriptionRepository inscriptionRepository = mock(InscriptionRepository.class);
    private final PerimetreService perimetreService = mock(PerimetreService.class);
    private final RegionService service =
            new RegionService(etablissementRepository, classeRepository, inscriptionRepository, perimetreService);

    private void connecter(AffectationInfo... affectations) {
        when(perimetreService.utilisateurCourant()).thenReturn(
                new UtilisateurPrincipal(1L, "r@example.org", "x", true, List.of(affectations)));
    }

    private static Etablissement etablissement(long id, String statut) {
        Etablissement e = new Etablissement();
        e.setId(id);
        e.setStatut(statut);
        return e;
    }

    @Test
    void laSyntheseNePorteQueSurLesEtablissementsDeLaZone() {
        connecter(new AffectationInfo(Role.DIRECTION_REGIONALE, Perimetre.ZONE, 7L, null));
        when(etablissementRepository.findByZoneEtSousZones(7L))
                .thenReturn(List.of(etablissement(10L, "ACTIF"), etablissement(11L, "SUSPENDU")));
        when(classeRepository.countByEtablissementIdInAndAnneeScolaireId(anyCollection(), eq(ANNEE))).thenReturn(6L);
        when(inscriptionRepository.countByEtablissementIdInAndAnneeScolaireIdAndStatut(
                anyCollection(), eq(ANNEE), eq("ACCEPTEE"))).thenReturn(150L);

        SyntheseNationaleDto s = service.synthese(ANNEE);

        assertThat(s.etablissements()).isEqualTo(2L);
        assertThat(s.actifs()).isEqualTo(1L);
        assertThat(s.suspendus()).isEqualTo(1L);
        assertThat(s.classes()).isEqualTo(6L);
        assertThat(s.elevesInscrits()).isEqualTo(150L);
    }

    @Test
    void unAutreRoleNElargitPasLePerimetreRegional() {
        connecter(new AffectationInfo(Role.ENSEIGNANT, Perimetre.ETABLISSEMENT, null, 10L));

        SyntheseNationaleDto s = service.synthese(ANNEE);

        assertThat(s.etablissements()).isZero();
        verify(etablissementRepository, never()).findByZoneEtSousZones(eq(10L));
        verify(classeRepository, never()).countByEtablissementIdInAndAnneeScolaireId(anyCollection(), eq(ANNEE));
    }
}
