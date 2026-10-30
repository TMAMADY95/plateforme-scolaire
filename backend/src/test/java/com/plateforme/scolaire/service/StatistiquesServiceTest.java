package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
    private final EtablissementRepository etablissements = mock(EtablissementRepository.class);
    private final Clock horloge = Clock.fixed(Instant.parse("2026-10-29T10:00:00Z"), ZoneOffset.UTC);
    private final StatistiquesService service = new StatistiquesService(repository,
            etablissements, classes, inscriptions, mock(RegionService.class), horloge);

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

    private static com.plateforme.scolaire.domain.ZoneGeographique zone(long id, String nom, String type,
            com.plateforme.scolaire.domain.ZoneGeographique parent) {
        var z = new com.plateforme.scolaire.domain.ZoneGeographique();
        z.setId(id);
        z.setNom(nom);
        z.setTypeZone(type);
        z.setParent(parent);
        return z;
    }

    private static com.plateforme.scolaire.domain.Etablissement ecole(long id, String nom,
            com.plateforme.scolaire.domain.ZoneGeographique zone) {
        var e = new com.plateforme.scolaire.domain.Etablissement();
        e.setId(id);
        e.setNom(nom);
        e.setCodeOfficiel("E" + id);
        e.setZone(zone);
        return e;
    }

    @Test
    void lesEffectifsSontDonnesParEtablissementEtAgregesParZone() {
        var pays = zone(1, "Pays", "Pays", null);
        var region = zone(2, "Région Nord", "Région", pays);
        var a = ecole(10, "École A", region);
        var b = ecole(11, "École B", pays);
        when(etablissements.findAll()).thenReturn(List.of(a, b));
        when(etablissements.findAllById(any())).thenReturn(List.of(a, b));
        when(repository.elevesParEtablissement(any(), eq(2L)))
                .thenReturn(List.<Object[]>of(new Object[] {10L, 120L}, new Object[] {11L, 30L}));
        when(repository.classesParEtablissement(any(), eq(2L)))
                .thenReturn(List.<Object[]>of(new Object[] {10L, 4L}));
        when(repository.personnelParEtablissement(any(), eq(com.plateforme.scolaire.domain.Role.ENSEIGNANT)))
                .thenReturn(List.<Object[]>of(new Object[] {10L, 9L}, new Object[] {11L, 2L}));
        when(repository.directionParEtablissement(any())).thenReturn(List.<Object[]>of(new Object[] {10L, 2L}));

        var lignes = service.lignesNationales(2L, null);
        assertThat(lignes).extracting("nom").containsExactly("École A", "École B");
        assertThat(lignes.get(0).eleves()).isEqualTo(120);
        assertThat(lignes.get(0).classes()).isEqualTo(4);
        assertThat(lignes.get(1).classes()).isZero();          // aucune classe : 0, pas d'absence de ligne
        assertThat(lignes.get(1).personnelDirection()).isZero();

        var zones = service.lignesParZone(2L);
        // La région compte l'école A ; le pays compte les deux.
        assertThat(zones).extracting("nom").containsExactly("Pays", "Région Nord");
        assertThat(zones.get(0).etablissements()).isEqualTo(2);
        assertThat(zones.get(0).eleves()).isEqualTo(150);
        assertThat(zones.get(0).enseignants()).isEqualTo(11);
        assertThat(zones.get(1).etablissements()).isEqualTo(1);
        assertThat(zones.get(1).eleves()).isEqualTo(120);
    }

    @Test
    void lesStatistiquesIncluentDirectionCycleEtSexe() {
        when(repository.compterPersonnelDirection(List.of(7L))).thenReturn(3L);
        when(repository.effectifsParCycle(List.of(7L), 2L))
                .thenReturn(List.<Object[]>of(new Object[] {"Primaire", 90L}, new Object[] {"Cycle non défini", 5L}));
        when(repository.effectifsParSexe(List.of(7L), 2L))
                .thenReturn(List.<Object[]>of(new Object[] {"F", 50L}, new Object[] {"M", 45L}));

        StatistiquesDto dto = service.etablissement(7L, 2L);

        assertThat(dto.personnelDirection()).isEqualTo(3);
        assertThat(dto.effectifsParCycle()).extracting("libelle").containsExactly("Primaire", "Cycle non défini");
        assertThat(dto.effectifsParSexe()).extracting("libelle").containsExactly("Filles", "Garçons");
    }
}
