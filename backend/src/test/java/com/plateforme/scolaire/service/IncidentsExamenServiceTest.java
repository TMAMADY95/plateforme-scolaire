package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.plateforme.scolaire.domain.CandidatExamen;
import com.plateforme.scolaire.domain.CentreExamen;
import com.plateforme.scolaire.domain.Eleve;
import com.plateforme.scolaire.domain.ExamenNational;
import com.plateforme.scolaire.repository.CandidatExamenRepository;
import com.plateforme.scolaire.repository.CentreExamenRepository;
import com.plateforme.scolaire.repository.EpreuveExamenRepository;
import com.plateforme.scolaire.repository.IncidentExamenRepository;
import com.plateforme.scolaire.web.dto.IncidentExamenRequest;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class IncidentsExamenServiceTest {

    private final IncidentExamenRepository incidents = mock(IncidentExamenRepository.class);
    private final CentreExamenRepository centres = mock(CentreExamenRepository.class);
    private final CandidatExamenRepository candidats = mock(CandidatExamenRepository.class);
    private final EpreuveExamenRepository epreuves = mock(EpreuveExamenRepository.class);
    private final IncidentsExamenService service = new IncidentsExamenService(incidents, centres, candidats, epreuves,
            mock(AuditService.class), Clock.fixed(Instant.parse("2027-06-10T08:00:00Z"), ZoneOffset.UTC));

    private CentreExamen centre(String statutResultats) {
        ExamenNational examen = new ExamenNational();
        examen.setId(7L);
        examen.setStatutResultats(statutResultats);
        CentreExamen c = new CentreExamen();
        c.setId(12L);
        c.setNom("Centre 12");
        c.setExamen(examen);
        when(centres.findById(12L)).thenReturn(Optional.of(c));
        return c;
    }

    private CandidatExamen candidat(CentreExamen centre) {
        Eleve e = new Eleve();
        e.setNom("Diallo");
        e.setPrenom("Aminata");
        CandidatExamen c = new CandidatExamen();
        c.setId(5L);
        c.setExamen(centre.getExamen());
        c.setCentre(centre);
        c.setEleve(e);
        c.setStatutEpreuves("PRESENT");
        c.setMoyenne(new BigDecimal("14.00"));
        c.setDecision("ADMIS");
        when(candidats.findById(5L)).thenReturn(Optional.of(c));
        when(candidats.duCentre(12L)).thenReturn(List.of(c));
        return c;
    }

    @Test
    void uneFraudeAvecEliminationDisqualifieLeCandidat() {
        CandidatExamen c = candidat(centre("EN_COURS"));

        service.signaler(7L, 12L, new IncidentExamenRequest("FRAUDE", 5L, null, "Téléphone portable sur la table", null, "Surveillant", true));

        assertThat(c.getStatutEpreuves()).isEqualTo("ELIMINE");
        assertThat(c.getDecision()).isEqualTo("NON_ADMIS");
        assertThat(c.getMoyenne()).isNull();
    }

    @Test
    void seuleUneFraudeImputeeAUnCandidatPeutEliminer() {
        candidat(centre("EN_COURS"));
        assertThatThrownBy(() -> service.signaler(7L, 12L, new IncidentExamenRequest("MALAISE", 5L, null, "Malaise", null, null, true)))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.signaler(7L, 12L, new IncidentExamenRequest("FRAUDE", null, null, "Bruit", null, null, true)))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void apresLaProclamationOnNElimineplus() {
        candidat(centre("PROCLAME"));
        assertThatThrownBy(() -> service.signaler(7L, 12L, new IncidentExamenRequest("FRAUDE", 5L, null, "Fraude", null, null, true)))
                .isInstanceOf(ResponseStatusException.class);
    }
}
