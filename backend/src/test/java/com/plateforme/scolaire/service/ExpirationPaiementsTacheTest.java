package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.plateforme.scolaire.repository.PaiementRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ExpirationPaiementsTacheTest {

    private final PaiementRepository repository = mock(PaiementRepository.class);
    private final PaiementService paiementService = mock(PaiementService.class);
    private final Clock horloge = Clock.fixed(Instant.parse("2026-10-29T10:00:00Z"), ZoneOffset.UTC);
    private final ExpirationPaiementsTache tache = new ExpirationPaiementsTache(repository, paiementService, horloge);

    @Test
    void uneErreurSurUnPaiementNeBloquePasLesAutres() {
        ReflectionTestUtils.setField(tache, "expirationHeures", 24L);
        Instant avant = Instant.parse("2026-10-28T10:00:00Z");
        when(repository.idsEnAttenteAvant(avant)).thenReturn(List.of(1L, 2L, 3L));
        when(paiementService.expirer(eq(1L), any())).thenReturn(true);
        when(paiementService.expirer(eq(2L), any())).thenThrow(new IllegalStateException("verrou"));
        when(paiementService.expirer(eq(3L), any())).thenReturn(false);

        assertThat(tache.expirerMaintenant()).isEqualTo(1);
    }
}
