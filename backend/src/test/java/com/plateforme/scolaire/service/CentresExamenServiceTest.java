package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.plateforme.scolaire.domain.CritereCentreExamen;
import com.plateforme.scolaire.domain.Etablissement;
import com.plateforme.scolaire.repository.CritereCentreExamenRepository;
import com.plateforme.scolaire.repository.SalleRepository;
import com.plateforme.scolaire.security.PerimetreService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class CentresExamenServiceTest {

    private static CritereCentreExamen criteres(String secteur, boolean autorisation, int salles, int capacite) {
        CritereCentreExamen c = new CritereCentreExamen();
        c.setSecteurAdmis(secteur);
        c.setAutorisationRequise(autorisation);
        c.setSallesMinimum(salles);
        c.setCapaciteMinimum(capacite);
        return c;
    }

    @Test
    void sansConditionToutEtablissementEstEligible() {
        assertThat(CentresExamenService.motifs("PRIVE", "EN_ATTENTE", 0, 0, criteres("TOUS", false, 0, 0))).isEmpty();
    }

    @Test
    void chaqueConditionNonRemplieDonneUnMotif() {
        var c = criteres("PUBLIC", true, 3, 100);

        var motifs = CentresExamenService.motifs("PRIVE", "EN_ATTENTE", 2, 60, c);

        assertThat(motifs).hasSize(4);
        assertThat(motifs.get(0)).contains("publics");
        assertThat(motifs.get(1)).contains("Autorisation");
        assertThat(motifs.get(2)).contains("3 salle(s)").contains("2 enregistrée(s)");
        assertThat(motifs.get(3)).contains("60 place(s) sur 100");
    }

    @Test
    void unEtablissementConformeEstEligible() {
        assertThat(CentresExamenService.motifs("PUBLIC", "AUTORISE", 3, 120, criteres("PUBLIC", true, 3, 100))).isEmpty();
    }

    @Test
    void laDesignationEstRefuseeSiLesConditionsNeSontPasRemplies() {
        var repo = mock(CritereCentreExamenRepository.class);
        var salles = mock(SalleRepository.class);
        when(repo.findById(1L)).thenReturn(Optional.of(criteres("TOUS", false, 2, 0)));
        when(salles.totaux(5L)).thenReturn(List.<Object[]>of(new Object[] {1L, 40L}));
        var service = new CentresExamenService(repo, salles, mock(PerimetreService.class), mock(AuditService.class));
        Etablissement e = new Etablissement();
        e.setId(5L);
        e.setNom("École A");

        assertThatThrownBy(() -> service.verifierEligible(e))
                .isInstanceOfSatisfying(ResponseStatusException.class, ex -> {
                    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(ex.getReason()).contains("École A").contains("2 salle(s)");
                });
    }

    @Test
    void unSecteurInconnuEstRefuse() {
        var repo = mock(CritereCentreExamenRepository.class);
        when(repo.findById(1L)).thenReturn(Optional.of(new CritereCentreExamen()));
        var service = new CentresExamenService(repo, mock(SalleRepository.class), mock(PerimetreService.class), mock(AuditService.class));

        assertThatThrownBy(() -> service.modifier(new com.plateforme.scolaire.web.dto.CriteresCentreRequest("AUTRE", false, 0, 0)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }
}
