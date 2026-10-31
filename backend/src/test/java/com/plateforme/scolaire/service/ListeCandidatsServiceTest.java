package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.plateforme.scolaire.domain.ExamenNational;
import com.plateforme.scolaire.domain.ListeCandidats;
import com.plateforme.scolaire.repository.CandidatExamenRepository;
import com.plateforme.scolaire.repository.EtablissementRepository;
import com.plateforme.scolaire.repository.ExamenNationalRepository;
import com.plateforme.scolaire.repository.ListeCandidatsRepository;
import com.plateforme.scolaire.repository.PieceCandidatureRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class ListeCandidatsServiceTest {

    private final ListeCandidatsRepository listes = mock(ListeCandidatsRepository.class);
    private final CandidatExamenRepository candidats = mock(CandidatExamenRepository.class);
    private final ExamenNationalRepository examens = mock(ExamenNationalRepository.class);
    private final ListeCandidatsService service = new ListeCandidatsService(listes, candidats, mock(PieceCandidatureRepository.class),
            examens, mock(EtablissementRepository.class), mock(AuditService.class),
            Clock.fixed(Instant.parse("2027-03-01T08:00:00Z"), ZoneOffset.UTC));

    private ListeCandidats liste(String statut) {
        ExamenNational examen = new ExamenNational();
        examen.setId(7L);
        when(examens.findById(7L)).thenReturn(Optional.of(examen));
        ListeCandidats l = new ListeCandidats();
        l.setExamen(examen);
        l.setStatut(statut);
        when(listes.findByExamenIdAndEtablissementId(7L, 3L)).thenReturn(Optional.of(l));
        when(candidats.deLEtablissement(7L, 3L)).thenReturn(List.of());
        return l;
    }

    @Test
    void uneListeSansCandidatNeSeTransmetPas() {
        liste("BROUILLON");
        assertThatThrownBy(() -> service.transmettre(3L, 7L)).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void uneListeTransmiseEstVerrouillee() {
        liste("TRANSMISE");
        assertThat(service.verrouillee(7L, 3L)).isTrue();
    }

    @Test
    void uneListeEnBrouillonNestPasVerrouillee() {
        liste("BROUILLON");
        assertThat(service.verrouillee(7L, 3L)).isFalse();
    }

    @Test
    void laValidationRegionaleExigeUneListeTransmise() {
        liste("BROUILLON");
        assertThatThrownBy(() -> service.validerRegion(3L, 7L)).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void unRejetRenvoieLaListeEnBrouillonAvecSonMotif() {
        ListeCandidats l = liste("TRANSMISE");
        service.rejeter(3L, 7L, "Acte de naissance illisible");
        assertThat(l.getStatut()).isEqualTo("BROUILLON");
        assertThat(l.getMotifRejet()).isEqualTo("Acte de naissance illisible");
    }

    @Test
    void unRejetSansMotifEstRefuse() {
        liste("TRANSMISE");
        assertThatThrownBy(() -> service.rejeter(3L, 7L, " ")).isInstanceOf(ResponseStatusException.class);
    }
}
