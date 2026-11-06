package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.plateforme.scolaire.domain.FicheInfrastructure;
import com.plateforme.scolaire.web.dto.InfrastructureDto;
import org.junit.jupiter.api.Test;

class InfrastructureServiceTest {

    private static FicheInfrastructure fiche(int dur, int prov, int places, int lf, int lg, String eau) {
        FicheInfrastructure f = new FicheInfrastructure();
        f.setSallesDur(dur);
        f.setSallesProvisoires(prov);
        f.setPlacesAssises(places);
        f.setLatrinesFilles(lf);
        f.setLatrinesGarcons(lg);
        f.setEau(eau);
        return f;
    }

    @Test
    void ratioArrondiEtNulSansDiviseur() {
        assertThat(InfrastructureService.ratio(100, 3)).isEqualTo(33.3);
        assertThat(InfrastructureService.ratio(100, 0)).isNull();
    }

    @Test
    void ecoleConformeSansAlerte() {
        InfrastructureDto.Indicateurs i = InfrastructureService.indicateurs(fiche(6, 0, 300, 4, 4, "FORAGE"), 240, 120, 120);
        assertThat(i.elevesParSalle()).isEqualTo(40.0);
        assertThat(i.alertes()).isEmpty();
    }

    @Test
    void sallesSurchargeesEtPlacesInsuffisantes() {
        InfrastructureDto.Indicateurs i = InfrastructureService.indicateurs(fiche(2, 1, 100, 4, 4, "PUITS"), 240, 120, 120);
        assertThat(i.elevesParSalle()).isEqualTo(80.0);
        assertThat(i.alertes()).anyMatch(a -> a.startsWith("Salles surchargées")).anyMatch(a -> a.startsWith("Places assises insuffisantes"));
    }

    @Test
    void latrinesEtEau() {
        InfrastructureDto.Indicateurs i = InfrastructureService.indicateurs(fiche(6, 0, 300, 0, 2, "AUCUNE"), 220, 100, 120);
        assertThat(i.alertes()).contains("Aucune latrine pour les filles", "Pas de point d'eau")
                .anyMatch(a -> a.startsWith("Latrines garçons insuffisantes"));
    }

    @Test
    void aucuneSalleAvecDesEleves() {
        InfrastructureDto.Indicateurs i = InfrastructureService.indicateurs(fiche(0, 0, 0, 1, 1, "RESEAU"), 30, 15, 15);
        assertThat(i.elevesParSalle()).isNull();
        assertThat(i.alertes()).contains("Aucune salle de classe déclarée");
    }
}
