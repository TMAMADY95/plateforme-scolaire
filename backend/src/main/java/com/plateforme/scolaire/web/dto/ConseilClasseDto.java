package com.plateforme.scolaire.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Conseil de classe de fin d'année : moyennes par période, moyenne annuelle, suggestion et décision de chaque élève. */
public record ConseilClasseDto(Long classeId, String classe, String niveau, List<Periode> periodes, Resume resume, List<Ligne> eleves) {

    public record Periode(Long id, String libelle) {
    }

    /** tauxPassage : passages / décisions prises, en pourcentage ; null sans décision. */
    public record Resume(int effectif, int evalues, int admis, int aDeliberer, int redouble, int decides, int passages, int redoublements, int exclusions,
            BigDecimal tauxPassage) {
    }

    /** suggestion : ADMIS (≥ 10), A_DELIBERER (≥ 8) ou REDOUBLE ; mention : FELICITATIONS, ENCOURAGEMENTS, TABLEAU_HONNEUR ou null. */
    public record Ligne(Long inscriptionId, EleveDto eleve, Map<Long, BigDecimal> moyennesPeriodes, BigDecimal moyenneAnnuelle, Integer rang, String suggestion,
            String mention, String decision, String observation) {
    }

    public record Decision(@NotNull Long inscriptionId, @Size(max = 12) String decision, @Size(max = 500) String observation) {
    }

    /** decision null : retire la décision déjà prise. */
    public record Requete(@NotEmpty List<@Valid Decision> decisions) {
    }
}
