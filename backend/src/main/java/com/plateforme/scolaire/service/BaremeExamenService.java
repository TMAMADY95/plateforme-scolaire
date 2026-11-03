package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.BaremeExamen;
import com.plateforme.scolaire.domain.ExamenNational;
import com.plateforme.scolaire.repository.CandidatExamenRepository;
import com.plateforme.scolaire.repository.ExamenNationalRepository;
import com.plateforme.scolaire.web.dto.ExamenDto;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Changement du barème d'un examen (10 ou 20) : possible seulement tant qu'aucun résultat n'est saisi. */
@Service
@RequiredArgsConstructor
@Transactional
public class BaremeExamenService {

    private final ExamenNationalRepository examens;
    private final CandidatExamenRepository candidats;
    private final AuditService audit;

    /** Pur : un barème valide vaut 10 ou 20. */
    static boolean valide(int bareme) {
        return bareme == BaremeExamen.SUR_10 || bareme == BaremeExamen.SUR_20;
    }

    public ExamenDto definir(Long examenId, int bareme) {
        if (!valide(bareme)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le barème vaut 10 ou 20");
        }
        ExamenNational examen = examens.findById(examenId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Examen introuvable"));
        if (!"EN_COURS".equals(examen.getStatutResultats())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Les résultats sont déjà délibérés : le barème ne peut plus changer");
        }
        if (examen.getBareme() != bareme && candidats.avecResultats(examenId) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Des notes ou des statuts sont déjà saisis : le barème ne peut plus changer");
        }
        int avant = examen.getBareme();
        examen.setBareme(bareme);
        audit.enregistrer("EXAMEN_BAREME_DEFINI", "ExamenNational", examenId, Map.of("avant", avant, "apres", bareme));
        return ExamenDto.de(examen);
    }
}
