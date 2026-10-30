package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.plateforme.scolaire.domain.CycleScolaire;
import com.plateforme.scolaire.repository.CycleScolaireRepository;
import com.plateforme.scolaire.web.dto.CycleDto;
import com.plateforme.scolaire.web.dto.CycleRequest;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class CycleServiceTest {

    private final CycleScolaireRepository repository = mock(CycleScolaireRepository.class);
    private final CycleService service = new CycleService(repository);

    private static CycleScolaire cycle(long id, String code) {
        CycleScolaire c = new CycleScolaire();
        c.setId(id);
        c.setCode(code);
        c.setLibelle(code);
        c.setOrdre(1);
        return c;
    }

    @Test
    void leCodeEstNormaliseEtUnique() {
        when(repository.save(any(CycleScolaire.class))).thenAnswer(i -> i.getArgument(0));
        CycleDto dto = service.creer(new CycleRequest(" maternelle ", " Maternelle ", 0 + 1));
        assertThat(dto.code()).isEqualTo("MATERNELLE");
        assertThat(dto.libelle()).isEqualTo("Maternelle");

        when(repository.findByCode("LYCEE")).thenReturn(Optional.of(cycle(3, "LYCEE")));
        assertThatThrownBy(() -> service.creer(new CycleRequest("lycee", "Lycée", 3)))
                .isInstanceOf(ResponseStatusException.class);
        verify(repository, org.mockito.Mockito.times(1)).save(any(CycleScolaire.class));
    }

    @Test
    void onPeutRenommerUnCycleMaisPasPrendreLeCodeDUnAutre() {
        CycleScolaire college = cycle(2, "COLLEGE");
        when(repository.findById(2L)).thenReturn(Optional.of(college));
        when(repository.findByCode("COLLEGE")).thenReturn(Optional.of(college));

        assertThat(service.modifier(2L, new CycleRequest("COLLEGE", "Collège (1er cycle)", 2)).libelle())
                .isEqualTo("Collège (1er cycle)");

        when(repository.findByCode("LYCEE")).thenReturn(Optional.of(cycle(3, "LYCEE")));
        assertThatThrownBy(() -> service.modifier(2L, new CycleRequest("LYCEE", "X", 2)))
                .isInstanceOf(ResponseStatusException.class);
    }
}
