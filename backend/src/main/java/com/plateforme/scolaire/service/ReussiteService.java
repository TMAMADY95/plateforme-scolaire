package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.Classe;
import com.plateforme.scolaire.repository.ClasseRepository;
import com.plateforme.scolaire.web.dto.LigneResultatDto;
import com.plateforme.scolaire.web.dto.ResultatsClasseDto;
import com.plateforme.scolaire.web.dto.ReussiteDto;
import com.plateforme.scolaire.web.dto.ReussiteDto.ReussiteClasseDto;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Taux de réussite par classe, calculé avec les mêmes moyennes que les bulletins (coefficients compris). */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReussiteService {

    /** Moyenne générale (sur 20) à atteindre pour être compté comme ayant réussi. */
    public static final BigDecimal SEUIL = BigDecimal.TEN;

    private final ClasseRepository classeRepository;
    private final MoyennesService moyennesService;

    public ReussiteDto reussite(Long etablissementId, Long anneeScolaireId, Long periodeId) {
        List<ReussiteClasseDto> classes = new ArrayList<>();
        long evalues = 0;
        long admis = 0;
        for (Classe classe : classeRepository
                .findByEtablissementIdAndAnneeScolaireIdOrderByNom(etablissementId, anneeScolaireId)) {
            ResultatsClasseDto resultats = moyennesService.resultatsClasse(classe.getId(), periodeId);
            List<LigneResultatDto> notes = resultats.eleves().stream()
                    .filter(l -> l.moyenneGenerale() != null).toList();
            long reussis = notes.stream().filter(l -> l.moyenneGenerale().compareTo(SEUIL) >= 0).count();
            evalues += notes.size();
            admis += reussis;
            classes.add(new ReussiteClasseDto(classe.getId(), classe.getNom(), classe.getNiveau().getLibelle(),
                    notes.size(), reussis, taux(reussis, notes.size()), resultats.moyenneGeneraleClasse()));
        }
        return new ReussiteDto(etablissementId, anneeScolaireId, periodeId, SEUIL, evalues, admis,
                taux(admis, evalues), classes);
    }

    /** Pourcentage à une décimale ; null quand il n'y a personne à évaluer. */
    static BigDecimal taux(long admis, long evalues) {
        if (evalues == 0) {
            return null;
        }
        return BigDecimal.valueOf(admis * 100).divide(BigDecimal.valueOf(evalues), 1, RoundingMode.HALF_UP);
    }
}
