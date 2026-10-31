package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.plateforme.scolaire.domain.ExamenNational;
import com.plateforme.scolaire.repository.*;
import com.plateforme.scolaire.security.PerimetreService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class ExamenSuppressionTest {

    private ExamenNationalRepository examens;
    private ExamenService service;
    private ExamenNational examen;

    @BeforeEach
    void preparer() {
        examens = mock(ExamenNationalRepository.class);
        service = new ExamenService(examens, mock(ResultatExamenRepository.class), mock(AnneeScolaireRepository.class),
                mock(CycleScolaireRepository.class), mock(NiveauRepository.class), mock(ZoneGeographiqueRepository.class),
                mock(EtablissementRepository.class), mock(RegionService.class), mock(AuditService.class),
                mock(PerimetreService.class), mock(TypeEtablissementRepository.class));
        examen = new ExamenNational();
        examen.setLibelle("BEPC");
        when(examens.findById(1L)).thenReturn(Optional.of(examen));
    }

    @Test
    void unExamenSansDonneesEstSupprimeDirectement() {
        service.supprimer(1L, false);

        verify(examens).delete(examen);
    }

    @Test
    void desCandidatsOuDesResultatsDemandentUneConfirmation() {
        when(examens.nombreCandidats(1L)).thenReturn(12L);

        assertThatThrownBy(() -> service.supprimer(1L, false))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        verify(examens, never()).delete(any(ExamenNational.class));
    }

    @Test
    void laConfirmationSupprimeAvecSesDonnees() {
        when(examens.nombreCandidats(1L)).thenReturn(12L);
        when(examens.nombreResultats(1L)).thenReturn(3L);

        service.supprimer(1L, true);

        verify(examens).delete(examen);
    }

    @Test
    void unExamenInconnuDonne404() {
        assertThatThrownBy(() -> service.supprimer(99L, true))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}
