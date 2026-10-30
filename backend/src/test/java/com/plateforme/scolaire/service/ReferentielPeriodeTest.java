package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.plateforme.scolaire.domain.AnneeScolaire;
import com.plateforme.scolaire.domain.Periode;
import com.plateforme.scolaire.repository.AnneeScolaireRepository;
import com.plateforme.scolaire.repository.MatiereRepository;
import com.plateforme.scolaire.repository.NiveauRepository;
import com.plateforme.scolaire.repository.PeriodeRepository;
import com.plateforme.scolaire.web.dto.PeriodeDto;
import com.plateforme.scolaire.web.dto.PeriodeRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class ReferentielPeriodeTest {

    private final PeriodeRepository periodes = mock(PeriodeRepository.class);
    private final ReferentielService service = new ReferentielService(
            mock(AnneeScolaireRepository.class), periodes, mock(NiveauRepository.class), mock(MatiereRepository.class),
            mock(com.plateforme.scolaire.repository.CycleScolaireRepository.class));

    private Periode p1;
    private Periode p2;

    private static Periode periode(long id, int ordre, AnneeScolaire annee) {
        Periode p = new Periode();
        p.setId(id);
        p.setAnneeScolaire(annee);
        p.setLibelle("Période " + ordre);
        p.setOrdre(ordre);
        p.setDateDebut(LocalDate.parse(ordre == 1 ? "2026-09-01" : "2027-01-05"));
        p.setDateFin(LocalDate.parse(ordre == 1 ? "2026-12-15" : "2027-03-30"));
        return p;
    }

    @BeforeEach
    void preparer() {
        AnneeScolaire annee = new AnneeScolaire();
        annee.setId(1L);
        annee.setDateDebut(LocalDate.parse("2026-09-01"));
        annee.setDateFin(LocalDate.parse("2027-06-30"));
        p1 = periode(10, 1, annee);
        p2 = periode(11, 2, annee);
        when(periodes.findById(10L)).thenReturn(Optional.of(p1));
        when(periodes.findByAnneeScolaireIdOrderByOrdre(1L)).thenReturn(List.of(p1, p2));
    }

    @Test
    void onPeutRenommerUnePeriode() {
        PeriodeDto dto = service.modifierPeriode(10L, new PeriodeRequest("Trimestre 1", 1,
                LocalDate.parse("2026-09-01"), LocalDate.parse("2026-12-15")));
        assertThat(dto.libelle()).isEqualTo("Trimestre 1");
        assertThat(p1.getLibelle()).isEqualTo("Trimestre 1");
    }

    @Test
    void lOrdreDUneAutrePeriodeEstRefuse() {
        assertThatThrownBy(() -> service.modifierPeriode(10L, new PeriodeRequest("X", 2,
                LocalDate.parse("2026-09-01"), LocalDate.parse("2026-12-15"))))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("numéro d'ordre");
    }

    @Test
    void lesDatesDoiventResterDansLAnnee() {
        assertThatThrownBy(() -> service.modifierPeriode(10L, new PeriodeRequest("X", 1,
                LocalDate.parse("2026-08-01"), LocalDate.parse("2026-12-15"))))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("comprise dans l'année");
    }

    @Test
    void unePeriodeInconnueEstIntrouvable() {
        assertThatThrownBy(() -> service.modifierPeriode(99L, new PeriodeRequest("X", 1,
                LocalDate.parse("2026-09-01"), LocalDate.parse("2026-12-15"))))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("introuvable");
    }
}
