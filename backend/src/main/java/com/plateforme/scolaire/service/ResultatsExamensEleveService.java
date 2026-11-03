package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.CandidatExamen;
import com.plateforme.scolaire.domain.EpreuveExamen;
import com.plateforme.scolaire.web.dto.ResultatExamenEleveDto;
import com.plateforme.scolaire.repository.CandidatExamenRepository;
import com.plateforme.scolaire.repository.EpreuveExamenRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Résultats des examens nationaux consultables par l'élève et ses parents. Rien n'est visible avant la proclamation
 * officielle par le ministère : ni la décision, ni la moyenne, ni les notes.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResultatsExamensEleveService {

    private final CandidatExamenRepository candidats;
    private final EpreuveExamenRepository epreuves;

    /** Pur : les résultats ne sont publics qu'une fois l'examen proclamé. */
    static boolean visible(String statutResultats) {
        return "PROCLAME".equals(statutResultats);
    }

    public List<ResultatExamenEleveDto> deLEleve(Long eleveId) {
        return candidats.deLEleve(eleveId).stream().filter(c -> visible(c.getExamen().getStatutResultats())).map(this::dto).toList();
    }

    private ResultatExamenEleveDto dto(CandidatExamen c) {
        var examen = c.getExamen();
        String statut = c.getStatutEpreuves();
        boolean present = "PRESENT".equals(statut);
        List<ResultatExamenEleveDto.NoteEpreuve> notes = present
                ? epreuves.findByExamenIdOrderByDateEpreuveAscHeureDebutAsc(examen.getId()).stream()
                        .filter(e -> c.getNotes().get(e.getId()) != null)
                        .map(e -> new ResultatExamenEleveDto.NoteEpreuve(e.nom(), e.getCoefficient(), c.getNotes().get(e.getId())))
                        .toList()
                : List.of();
        String mention = "ADMIS".equals(c.getDecision()) ? com.plateforme.scolaire.domain.BaremeExamen.mention(c.getMoyenne(), examen.getBareme()) : null;
        return new ResultatExamenEleveDto(examen.getId(), examen.getLibelle(), examen.getAnneeScolaire().getLibelle(),
                examen.getBareme(), examen.getDateProclamation(), c.getNumero(), statut, c.getDecision(), present ? c.getMoyenne() : null, mention, notes);
    }
}
