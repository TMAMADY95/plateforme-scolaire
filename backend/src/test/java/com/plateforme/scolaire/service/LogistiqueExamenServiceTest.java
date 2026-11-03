package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.plateforme.scolaire.web.dto.LogistiqueExamenDto.LigneCentre;
import com.plateforme.scolaire.web.dto.LogistiqueExamenDto.Livraison;
import com.plateforme.scolaire.web.dto.PersonnelCentreDto.Besoin;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class LogistiqueExamenServiceTest {

    private static Livraison l(String article, String statut) {
        return new Livraison(article, statut, 10, null, null, null);
    }

    @Test
    void postesPourvusIgnorentLesSurnombres() {
        assertThat(LogistiqueExamenService.pourvus(List.of(new Besoin("a", 2, 3), new Besoin("b", 1, 0), new Besoin("c", 2, 1)))).isEqualTo(3);
    }

    @Test
    void approvisionneSeulementSiToutEstRecu() {
        var tout = List.of(l("SUJETS", "RECUE"), l("COPIES", "RECUE"), l("ENVELOPPES", "RECUE"), l("FOURNITURES", "RECUE"));
        assertThat(LogistiqueExamenService.approvisionne(tout)).isTrue();
        assertThat(LogistiqueExamenService.approvisionne(List.of(l("SUJETS", "RECUE"), l("COPIES", "EXPEDIEE")))).isFalse();
        assertThat(LogistiqueExamenService.approvisionne(List.of())).isFalse();
    }

    @Test
    void ecartEtRetard() {
        assertThat(LogistiqueExamenService.ecart(100, 95)).isEqualTo(-5);
        assertThat(LogistiqueExamenService.ecart(100, null)).isNull();
        LocalDate debut = LocalDate.of(2026, 6, 10);
        assertThat(LogistiqueExamenService.enRetard(false, debut, debut)).isTrue();
        assertThat(LogistiqueExamenService.enRetard(false, debut, debut.minusDays(1))).isFalse();
        assertThat(LogistiqueExamenService.enRetard(true, debut, debut.plusDays(3))).isFalse();
        assertThat(LogistiqueExamenService.enRetard(false, null, debut)).isFalse();
    }

    @Test
    void resumeDuSuivi() {
        var a = new LigneCentre(1L, "A", "Z", 2, 7, 7, true, List.of(), true, true, false);
        var b = new LigneCentre(2L, "B", "Z", 3, 9, 3, false, List.of(), false, false, true);
        var r = LogistiqueExamenService.resume(List.of(a, b));
        assertThat(r.centres()).isEqualTo(2);
        assertThat(r.prets()).isEqualTo(1);
        assertThat(r.enRetard()).isEqualTo(1);
        assertThat(r.postesRequis()).isEqualTo(16);
        assertThat(r.postesPourvus()).isEqualTo(10);
        assertThat(r.tauxPostesPourvus()).isEqualTo(62.5);
        assertThat(r.tauxPrets()).isEqualTo(50.0);
    }
}
