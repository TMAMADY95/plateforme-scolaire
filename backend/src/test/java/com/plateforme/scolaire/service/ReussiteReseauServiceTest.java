package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.plateforme.scolaire.domain.Etablissement;
import com.plateforme.scolaire.domain.ZoneGeographique;
import com.plateforme.scolaire.repository.EtablissementRepository;
import com.plateforme.scolaire.repository.ReussiteReseauRepository;
import com.plateforme.scolaire.web.dto.ReussiteReseauDto;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ReussiteReseauServiceTest {

    private final ReussiteReseauRepository repository = mock(ReussiteReseauRepository.class);
    private final EtablissementRepository etablissements = mock(EtablissementRepository.class);
    private final RegionService region = mock(RegionService.class);
    private final ReussiteReseauService service = new ReussiteReseauService(repository, etablissements, region);

    private static Etablissement etablissement(long id, String nom, String zone) {
        Etablissement e = new Etablissement();
        e.setId(id);
        e.setNom(nom);
        if (zone != null) {
            ZoneGeographique z = new ZoneGeographique();
            z.setNom(zone);
            e.setZone(z);
        }
        return e;
    }

    private static Object[] note(long classe, long inscription, long matiere, String valeur) {
        return new Object[] {classe, inscription, matiere, new BigDecimal(valeur), new BigDecimal("20"), BigDecimal.ONE};
    }

    @Test
    void leTauxRegroupeParNiveauParZoneEtParEtablissement() {
        Etablissement a = etablissement(1L, "Collège A", "Nord");
        Etablissement b = etablissement(2L, "Collège B", null);
        when(etablissements.findAll()).thenReturn(List.of(a, b));
        List<Object[]> classes = List.<Object[]>of(new Object[] {10L, 1L, "6e", 1}, new Object[] {20L, 2L, "6e", 1});
        when(repository.classes(List.of(1L, 2L), 2L)).thenReturn(classes);
        when(repository.coefficients(List.of(1L, 2L))).thenReturn(List.of());
        // classe 10 : un élève à 14, un à 6 ; classe 20 : un élève à 12
        when(repository.notes(List.of(1L, 2L), 5L)).thenReturn(List.<Object[]>of(
                note(10L, 100L, 1L, "14"), note(10L, 101L, 1L, "6"), note(20L, 200L, 1L, "12")));

        ReussiteReseauDto dto = service.nationale(2L, 5L);

        assertThat(dto.evalues()).isEqualTo(3);
        assertThat(dto.admis()).isEqualTo(2);
        assertThat(dto.taux()).isEqualByComparingTo("66.7");
        assertThat(dto.parNiveau()).hasSize(1);
        assertThat(dto.parNiveau().get(0).libelle()).isEqualTo("6e");
        assertThat(dto.parZone()).extracting("libelle").containsExactly("Sans zone", "Nord");
        assertThat(dto.etablissements().get(0).libelle()).isEqualTo("Collège B");
        assertThat(dto.etablissements().get(0).taux()).isEqualByComparingTo("100.0");
    }

    @Test
    void lesNotesDUneClasseHorsAnneeSontIgnorees() {
        when(etablissements.findAll()).thenReturn(List.of(etablissement(1L, "Collège A", "Nord")));
        when(repository.classes(List.of(1L), 2L)).thenReturn(List.of());
        when(repository.notes(List.of(1L), 5L)).thenReturn(List.<Object[]>of(note(10L, 100L, 1L, "14")));

        ReussiteReseauDto dto = service.nationale(2L, 5L);

        assertThat(dto.evalues()).isZero();
        assertThat(dto.taux()).isNull();
    }

    @Test
    void sansEtablissementLaReponseEstVide() {
        when(region.etablissementsDeLaRegion()).thenReturn(Map.of());

        ReussiteReseauDto dto = service.regionale(2L, 5L);

        assertThat(dto.taux()).isNull();
        assertThat(dto.etablissements()).isEmpty();
    }

    @Test
    void lePremierCalculEstGardeEnMemoire() {
        when(etablissements.findAll()).thenReturn(List.of(etablissement(1L, "Collège A", "Nord")));
        when(repository.classes(List.of(1L), 2L)).thenReturn(List.of());
        when(repository.notes(List.of(1L), 5L)).thenReturn(List.of());

        ReussiteReseauDto premier = service.nationale(2L, 5L);
        ReussiteReseauDto second = service.nationale(2L, 5L);

        assertThat(second).isSameAs(premier);
        org.mockito.Mockito.verify(repository, org.mockito.Mockito.times(1)).notes(List.of(1L), 5L);
    }
}
