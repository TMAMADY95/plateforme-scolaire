package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.AlerteEnvoyee;
import com.plateforme.scolaire.repository.AlerteEnvoyeeRepository;
import com.plateforme.scolaire.web.dto.AlerteEnvoyeeDto;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Mémorise chaque alerte envoyée : en cas de litige, on sait qui a été prévenu, quand, par quel canal. */
@Component
@RequiredArgsConstructor
public class JournalAlertes {

    static final String TYPE_ABSENCE = "ABSENCE";

    private final AlerteEnvoyeeRepository repository;
    private final Clock clock;

    public void noter(String type, String canal, String destinataire, String resume, boolean reussie) {
        AlerteEnvoyee a = new AlerteEnvoyee();
        a.setEnvoyeeLe(Instant.now(clock));
        a.setType(type);
        a.setCanal(canal);
        a.setDestinataire(destinataire);
        a.setResume(resume.length() > 500 ? resume.substring(0, 499) + "…" : resume);
        a.setReussie(reussie);
        repository.save(a);
    }

    @Transactional(readOnly = true)
    public List<AlerteEnvoyeeDto> recentes() {
        return repository.findTop200ByTypeNotOrderByEnvoyeeLeDescIdDesc(TYPE_ABSENCE).stream().map(AlerteEnvoyeeDto::de).toList();
    }

    /** Alertes d'absence envoyées aux parents : visibles uniquement par l'établissement concerné. */
    @Transactional(readOnly = true)
    public List<AlerteEnvoyeeDto> absencesDeLEtablissement(Long etablissementId) {
        return repository.absencesDeLEtablissement(etablissementId).stream().map(AlerteEnvoyeeDto::de).toList();
    }
}
