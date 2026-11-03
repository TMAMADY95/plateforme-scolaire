package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.CandidatExamen;
import com.plateforme.scolaire.domain.CentreExamen;
import com.plateforme.scolaire.repository.CandidatExamenRepository;
import com.plateforme.scolaire.repository.CentreExamenRepository;
import com.plateforme.scolaire.repository.EpreuveCentreRepository;
import com.plateforme.scolaire.web.dto.ConcordanceRapportsDto;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Rapproche les rapports de surveillance d'un centre des statuts saisis sur les candidats : le total présents + absents
 * + éliminés doit égaler le nombre de candidats, les copies ramassées celles des présents et éliminés, et le rapport
 * ne peut compter moins de présents que de candidats déclarés présents. Lecture seule.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConcordanceRapportsService {

    private final CentreExamenRepository centres;
    private final CandidatExamenRepository candidats;
    private final EpreuveCentreRepository epreuvesCentre;

    /** Rapport saisi d'une épreuve (les nombres sont nuls tant que le rapport n'est pas renseigné). */
    record Rapport(Long epreuveId, String epreuve, Integer presents, Integer absents, Integer elimines, Integer copies, boolean clos) {}

    public ConcordanceRapportsDto concordance(Long examenId, Long centreId) {
        CentreExamen centre = centres.findById(centreId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Centre introuvable"));
        if (!centre.getExamen().getId().equals(examenId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Centre introuvable pour cet examen");
        }
        List<CandidatExamen> liste = candidats.duCentre(centreId);
        int presents = (int) liste.stream().filter(c -> "PRESENT".equals(c.getStatutEpreuves())).count();
        int absents = (int) liste.stream().filter(c -> "ABSENT".equals(c.getStatutEpreuves())).count();
        int elimines = (int) liste.stream().filter(c -> "ELIMINE".equals(c.getStatutEpreuves())).count();
        boolean complets = liste.stream().noneMatch(c -> "INSCRIT".equals(c.getStatutEpreuves()));
        List<Rapport> rapports = epreuvesCentre.findByCentreId(centreId).stream()
                .map(e -> new Rapport(e.getEpreuve().getId(), e.getEpreuve().nom(), e.getPresents(), e.getAbsents(), e.getElimines(),
                        e.getCopiesRamassees(), e.isRapportClos()))
                .sorted(java.util.Comparator.comparing(Rapport::epreuveId)).toList();
        return new ConcordanceRapportsDto(examenId, centreId, liste.size(), presents, absents, elimines, complets,
                ecarts(liste.size(), presents, elimines, complets, rapports));
    }

    /** Pur : écarts entre les rapports renseignés et les statuts des candidats du centre. */
    static List<ConcordanceRapportsDto.Ecart> ecarts(int candidats, int presents, int elimines, boolean statutsComplets,
            List<Rapport> rapports) {
        List<ConcordanceRapportsDto.Ecart> res = new ArrayList<>();
        for (Rapport r : rapports) {
            if (r.presents() == null || r.absents() == null || r.elimines() == null) {
                continue;
            }
            int total = r.presents() + r.absents() + r.elimines();
            if (total != candidats) {
                res.add(new ConcordanceRapportsDto.Ecart("TOTAL_INCOHERENT", "ERREUR", r.epreuveId(), r.epreuve(),
                        "Présents + absents + éliminés = " + total + " pour " + candidats + " candidats dans le centre"));
            }
            if (r.copies() != null && r.copies() != r.presents() + r.elimines()) {
                res.add(new ConcordanceRapportsDto.Ecart("COPIES_INCOHERENTES", r.clos() ? "ERREUR" : "AVERTISSEMENT", r.epreuveId(),
                        r.epreuve(), r.copies() + " copies ramassées pour " + (r.presents() + r.elimines()) + " présents et éliminés"));
            }
            if (statutsComplets && r.presents() < presents) {
                res.add(new ConcordanceRapportsDto.Ecart("PRESENTS_INFERIEURS", "ERREUR", r.epreuveId(), r.epreuve(),
                        "Le rapport compte " + r.presents() + " présents alors que " + presents + " candidats sont déclarés présents"));
            }
            if (statutsComplets && r.elimines() > elimines) {
                res.add(new ConcordanceRapportsDto.Ecart("ELIMINES_NON_REPORTES", "AVERTISSEMENT", r.epreuveId(), r.epreuve(),
                        "Le rapport compte " + r.elimines() + " éliminés mais " + elimines + " candidats ont le statut éliminé"));
            }
        }
        return res;
    }
}
