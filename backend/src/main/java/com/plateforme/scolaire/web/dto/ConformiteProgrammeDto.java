package com.plateforme.scolaire.web.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Compare les heures de l'emploi du temps d'une classe au programme officiel de son niveau (et série).
 * statut : CONFORME, MANQUE (moins d'heures que prévu), EXCES, NON_PLANIFIEE (au programme, absente de l'emploi du temps),
 * HORS_PROGRAMME (planifiée mais absente du programme), A_PRECISER (le programme n'indique pas d'horaire).
 */
public record ConformiteProgrammeDto(Long classeId, boolean programmeDisponible, BigDecimal heuresPrevues, BigDecimal heuresPlanifiees,
        List<Ligne> lignes) {

    public record Ligne(Long matiereId, String matiere, BigDecimal prevues, BigDecimal planifiees, BigDecimal ecart, String statut) {}
}
