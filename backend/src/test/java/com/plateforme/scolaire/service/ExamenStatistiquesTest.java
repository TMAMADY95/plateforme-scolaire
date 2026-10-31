package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.plateforme.scolaire.domain.Etablissement;
import com.plateforme.scolaire.domain.ZoneGeographique;
import com.plateforme.scolaire.web.dto.RapportExamenDto;
import com.plateforme.scolaire.web.dto.ResultatExamenRequest;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class ExamenStatistiquesTest {

    private static ZoneGeographique zone(long id, String type, String nom, ZoneGeographique parent) {
        ZoneGeographique z = new ZoneGeographique();
        z.setId(id);
        z.setTypeZone(type);
        z.setNom(nom);
        z.setParent(parent);
        return z;
    }

    private static Etablissement ecole(long id, ZoneGeographique zone) {
        Etablissement e = new Etablissement();
        e.setId(id);
        e.setZone(zone);
        return e;
    }

    private static RapportExamenDto.Ligne ligne(long id, int presentes, int admis, Integer absents, Integer elimines,
                                                Integer fillesP, Integer fillesA, List<ResultatExamenRequest.Serie> series) {
        return new RapportExamenDto.Ligne(id, "GN-" + id, "École " + id, "PUBLIC", null, null, presentes, admis,
                ExamenService.taux(admis, presentes), null, absents, elimines, fillesP, fillesA, series);
    }

    @Test
    void lesStatistiquesRegroupentParSexeRegionPrefectureEtSerie() {
        var region = zone(1, "REGION", "Kindia", null);
        var prefecture = zone(2, "PREFECTURE", "Coyah", region);
        var commune = zone(3, "COMMUNE", "Coyah-centre", prefecture);
        var lignes = List.of(
                ligne(1, 100, 60, 5, 1, 40, 22, List.of(new ResultatExamenRequest.Serie("SCIENCES_SOCIALES", 100, 60))),
                ligne(2, 50, 20, 2, 0, null, null, List.of()));
        var stats = ExamenService.statistiques(lignes, List.of(ecole(1, commune), ecole(2, commune)));

        assertThat(stats.absents()).isEqualTo(7);
        assertThat(stats.elimines()).isEqualTo(1);
        // Le sexe ne compte que l'école qui l'a renseigné : 22/40 filles, 38/60 garçons
        assertThat(stats.parSexe().get(0).taux()).isEqualByComparingTo(new BigDecimal("55.0"));
        assertThat(stats.parSexe().get(1).presentes()).isEqualTo(60);
        assertThat(stats.parRegion()).hasSize(1);
        assertThat(stats.parRegion().get(0).libelle()).isEqualTo("Kindia");
        assertThat(stats.parRegion().get(0).presentes()).isEqualTo(150);
        assertThat(stats.parPrefecture().get(0).libelle()).isEqualTo("Coyah");
        assertThat(stats.parSerie()).hasSize(1);
        assertThat(stats.parSerie().get(0).libelle()).isEqualTo("SCIENCES_SOCIALES");
    }

    @Test
    void lesDetailsIncoherentsSontRefuses() {
        assertThatThrownBy(() -> ExamenService.verifierDetails(new ResultatExamenRequest(100, 60, 0, 0, 120, 10, null)))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> ExamenService.verifierDetails(new ResultatExamenRequest(100, 60, 0, 0, 40, null, null)))
                .isInstanceOf(ResponseStatusException.class);
        // 60 admis dont 5 filles seulement : 55 garçons admis pour 100 - 90 = 10 garçons présents
        assertThatThrownBy(() -> ExamenService.verifierDetails(new ResultatExamenRequest(100, 60, 0, 0, 90, 5, null)))
                .isInstanceOf(ResponseStatusException.class);
        var series = List.of(new ResultatExamenRequest.Serie("SCIENCES_SOCIALES", 60, 30));
        assertThatThrownBy(() -> ExamenService.verifierDetails(new ResultatExamenRequest(100, 60, 0, 0, null, null, series)))
                .isInstanceOf(ResponseStatusException.class);
        ExamenService.verifierDetails(new ResultatExamenRequest(100, 60, 3, 1, 40, 22, null));   // cohérent : pas d'exception
    }
}
