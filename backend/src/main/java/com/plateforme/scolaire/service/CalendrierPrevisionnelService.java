package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.ExamenNational;
import com.plateforme.scolaire.repository.ExamenNationalRepository;
import com.plateforme.scolaire.web.dto.CalendrierPrevisionnelDto;
import com.plateforme.scolaire.web.dto.CalendrierPrevisionnelRequest;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Calendrier prévisionnel des suites d'un examen : correction, délibération, proclamation. Les dates doivent se
 * suivre dans l'ordre (fin des épreuves, correction, délibération, proclamation) ; les retards sont signalés.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CalendrierPrevisionnelService {

    private final ExamenNationalRepository examens;
    private final ExamenService examenService;
    private final AuditService audit;
    private final Clock clock;

    @Transactional(readOnly = true)
    public CalendrierPrevisionnelDto calendrier(Long examenId) {
        return dto(examen(examenId));
    }

    public CalendrierPrevisionnelDto definir(Long examenId, CalendrierPrevisionnelRequest r) {
        ExamenNational e = examen(examenId);
        examenService.verifierGestionCalendrier(e);
        verifierOrdre(e.getDateFin(), r.correctionDebut(), r.correctionFin(), r.deliberation(), r.proclamation());
        e.setCorrectionDebutPrevue(r.correctionDebut());
        e.setCorrectionFinPrevue(r.correctionFin());
        e.setDeliberationPrevue(r.deliberation());
        e.setProclamationPrevue(r.proclamation());
        audit.enregistrer("EXAMEN_CALENDRIER_PREVISIONNEL", "ExamenNational", examenId,
                Map.of("correctionDebut", String.valueOf(r.correctionDebut()), "correctionFin", String.valueOf(r.correctionFin()),
                        "deliberation", String.valueOf(r.deliberation()), "proclamation", String.valueOf(r.proclamation())));
        return dto(e);
    }

    /** Pur : chaque date renseignée ne doit pas précéder la précédente date renseignée de la chaîne. */
    static void verifierOrdre(LocalDate finEpreuves, LocalDate correctionDebut, LocalDate correctionFin,
            LocalDate deliberation, LocalDate proclamation) {
        String[] noms = {"la fin des épreuves", "le début de la correction", "la fin de la correction", "la délibération",
                "la proclamation"};
        LocalDate[] dates = {finEpreuves, correctionDebut, correctionFin, deliberation, proclamation};
        int precedent = -1;
        for (int i = 0; i < dates.length; i++) {
            if (dates[i] == null) {
                continue;
            }
            if (precedent >= 0 && dates[i].isBefore(dates[precedent])) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "La date de " + noms[i].replaceFirst("^(la|le) ", "") + " précède " + noms[precedent]);
            }
            precedent = i;
        }
    }

    /** Pur : étapes dont la date prévue est dépassée sans que l'étape soit franchie. */
    static List<String> retards(LocalDate aujourdhui, String statutResultats, LocalDate correctionFin, LocalDate deliberation,
            LocalDate proclamation) {
        List<String> r = new ArrayList<>();
        boolean enCours = "EN_COURS".equals(statutResultats);
        boolean proclame = "PROCLAME".equals(statutResultats);
        if (enCours && correctionFin != null && aujourdhui.isAfter(correctionFin)) {
            r.add("CORRECTION");
        }
        if (enCours && deliberation != null && aujourdhui.isAfter(deliberation)) {
            r.add("DELIBERATION");
        }
        if (!proclame && proclamation != null && aujourdhui.isAfter(proclamation)) {
            r.add("PROCLAMATION");
        }
        return r;
    }

    private CalendrierPrevisionnelDto dto(ExamenNational e) {
        return new CalendrierPrevisionnelDto(e.getId(), e.getDateFin(), e.getCorrectionDebutPrevue(), e.getCorrectionFinPrevue(),
                e.getDeliberationPrevue(), e.getProclamationPrevue(), e.getStatutResultats(), e.getDateDeliberation(),
                e.getDateProclamation(), retards(LocalDate.now(clock), e.getStatutResultats(), e.getCorrectionFinPrevue(),
                        e.getDeliberationPrevue(), e.getProclamationPrevue()));
    }

    private ExamenNational examen(Long id) {
        return examens.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Examen introuvable"));
    }
}
