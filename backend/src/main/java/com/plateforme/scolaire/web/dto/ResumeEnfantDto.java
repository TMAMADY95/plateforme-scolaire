package com.plateforme.scolaire.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Le point du jour sur un enfant. Les champs de moyenne sont nuls tant qu'il n'y a pas de note ; [periode] est la
 * période en cours (ou la dernière commencée). Les présences portent sur les 7 derniers jours, aujourd'hui compris.
 * [resteAPayer] additionne les factures non soldées ; il vaut null (ainsi que [devise]) quand elles sont dans des
 * devises différentes, car on ne les additionne pas : seul [facturesImpayees] est alors significatif.
 */
public record ResumeEnfantDto(Long eleveId, String prenom, String nom, String relation, Long classeId,
                              String classeNom, String periode, BigDecimal moyenneGenerale,
                              BigDecimal moyenneClasse, Integer rang, int effectif,
                              int absencesSemaine, int absencesNonJustifiees, int retardsSemaine,
                              List<DevoirResume> prochainsDevoirs,
                              int facturesImpayees, int facturesEnRetard, BigDecimal resteAPayer, String devise) {

    public record DevoirResume(Long id, String matiere, String titre, LocalDate dateLimite) {
    }
}
