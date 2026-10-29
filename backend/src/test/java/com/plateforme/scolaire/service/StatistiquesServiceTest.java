package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.plateforme.scolaire.repository.ClasseRepository;
import com.plateforme.scolaire.repository.EtablissementRepository;
import com.plateforme.scolaire.repository.InscriptionRepository;
import com.plateforme.scolaire.repository.StatistiquesRepository;
import com.plateforme.scolaire.web.dto.StatistiquesDto;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class StatistiquesServiceTest {

    private final StatistiquesRepository repository = mock(StatistiquesRepository.class);
    private final ClasseRepository classes = mock(ClasseRepository.class);
    private final InscriptionRepository inscriptions = mock(InscriptionRepository.class);
    private final Clock horloge = Clock.fixed(Instant.parse("2026-10-29T10:00:00Z"), ZoneOffset.UTC);
    private final StatistiquesService service = new StatistiquesService(repository,
            mock(EtablissementRepository.class), classes, inscriptions, mock(RegionService.class), horloge);

    @Test
    void leTauxDePresenceCompteLesRetardsCommePresentsEtVautNullSansDonnees() {
        List<Object[]> lignes = List.of(new Object[] {"PRESENT", 80L}, new Object[] {"RETARD", 5L},
                new Object[] {"ABSENT", 15L});

        assertThat(StatistiquesService.tauxPresence(lignes)).isEqualByComparingTo("85.0");
        assertThat(StatistiquesService.tauxPresence(List.of())).isNull();
    }

    @Test
    void leMoisEstFormateAvecUnZeroInitial() {
        assertThat(StatistiquesService.mois(2026, 9)).isEqualTo("2026-09");
    }

    @Test
    void lesStatistiquesAssemblentLesAgregatsDeLEtablissement() {
        when(repository.compterEnseignants(List.of(7L))).thenReturn(12L);
        when(classes.countByEtablissementIdInAndAnneeScolaireId(List.of(7L), 2L)).thenReturn(5L);
        when(inscriptions.countByEtablissementIdInAndAnneeScolaireIdAndStatut(List.of(7L), 2L, "ACCEPTEE"))
                .thenReturn(140L);
        when(repository.effectifsParNiveau(List.of(7L), 2L))
                .thenReturn(List.of(new Object[] {"6e", 60L}, new Object[] {"5e", 80L}));
        when(repository.inscriptionsParMois(List.of(7L), 2L))
                .thenReturn(List.<Object[]>of(new Object[] {2026, 9, 90L}, new Object[] {2026, 10, 50L}));
        when(repository.presencesDepuis(eq(List.of(7L)), eq(LocalDate.of(2026, 9, 29))))
                .thenReturn(List.<Object[]>of(new Object[] {"PRESENT", 9L}, new Object[] {"ABSENT", 1L}));

        StatistiquesDto dto = service.etablissement(7L, 2L);

        assertThat(dto.etablissements()).isEqualTo(1);
        assertThat(dto.enseignants()).isEqualTo(12);
        assertThat(dto.classes()).isEqualTo(5);
        assertThat(dto.elevesInscrits()).isEqualTo(140);
        assertThat(dto.tauxPresence()).isEqualByComparingTo("90.0");
        assertThat(dto.effectifsParNiveau()).extracting("libelle").containsExactly("6e", "5e");
        assertThat(dto.inscriptionsParMois()).extracting("mois").containsExactly("2026-09", "2026-10");
    }
}
