package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.plateforme.scolaire.domain.ExamenNational;
import com.plateforme.scolaire.domain.Role;
import com.plateforme.scolaire.security.UtilisateurPrincipal.AffectationInfo;
import com.plateforme.scolaire.domain.Perimetre;
import com.plateforme.scolaire.web.dto.EpreuveRequest;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.assertThat;

class EpreuvesExamenServiceTest {

    private static ExamenNational examen(String organisation) {
        ExamenNational e = new ExamenNational();
        e.setNiveauOrganisation(organisation);
        e.setDateDebut(LocalDate.of(2027, 6, 10));
        e.setDateFin(LocalDate.of(2027, 6, 14));
        return e;
    }

    private static EpreuveRequest epreuve(LocalDate date, String debut, String fin) {
        return new EpreuveRequest(5L, null, date, LocalTime.parse(debut), LocalTime.parse(fin));
    }

    @Test
    void uneEpreuveDansLaPeriodeAvecUnCreneauCoherentEstAcceptee() {
        assertThatCode(() -> EpreuvesExamenService.verifier(examen("NATIONAL"), epreuve(LocalDate.of(2027, 6, 11), "08:00", "11:00")))
                .doesNotThrowAnyException();
    }

    @Test
    void unCreneauInverseOuUneDateHorsPeriodeEstRefuse() {
        assertThatThrownBy(() -> EpreuvesExamenService.verifier(examen("NATIONAL"), epreuve(LocalDate.of(2027, 6, 11), "11:00", "08:00")))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> EpreuvesExamenService.verifier(examen("NATIONAL"), epreuve(LocalDate.of(2027, 6, 20), "08:00", "11:00")))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void uneEpreuveSansMatiereNiLibelleEstRefusee() {
        var sans = new EpreuveRequest(null, " ", LocalDate.of(2027, 6, 11), LocalTime.of(8, 0), LocalTime.of(10, 0));
        assertThatThrownBy(() -> EpreuvesExamenService.verifier(examen("NATIONAL"), sans)).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void laDirectionRegionaleNeGerePasLeCalendrierDUnExamenNational() {
        var region = List.of(new AffectationInfo(Role.DIRECTION_REGIONALE, Perimetre.ZONE, 1L, null));
        var ministere = List.of(new AffectationInfo(Role.MINISTERE, Perimetre.NATIONAL, null, null));

        assertThat(ExamenService.peutGererCalendrier(examen("NATIONAL"), region)).isFalse();
        assertThat(ExamenService.peutGererCalendrier(examen("REGION"), region)).isTrue();
        assertThat(ExamenService.peutGererCalendrier(examen("NATIONAL"), ministere)).isTrue();
    }
}
