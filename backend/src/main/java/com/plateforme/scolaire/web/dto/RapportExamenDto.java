package com.plateforme.scolaire.web.dto;

import java.math.BigDecimal;
import java.util.List;

/** Résultats d'un examen national pour un périmètre : synthèse, secteurs et détail par établissement. */
public record RapportExamenDto(ExamenDto examen, Synthese synthese, List<Synthese> parSecteur, List<Ligne> lignes,
                               Statistiques statistiques) {

    public RapportExamenDto(ExamenDto examen, Synthese synthese, List<Synthese> parSecteur, List<Ligne> lignes) {
        this(examen, synthese, parSecteur, lignes, null);
    }

    /** Un total ; « libelle » nomme le secteur dans la liste par secteur. */
    public record Synthese(String libelle, long etablissements, long transmis, long presentes, long admis, BigDecimal taux) {
    }

    public record Ligne(Long id, String codeOfficiel, String nom, String secteur, Long zoneId, String zone,
                        Integer presentes, Integer admis, BigDecimal taux, String organisateur,
                        Integer absents, Integer elimines, Integer presentesFilles, Integer admisFilles,
                        List<ResultatExamenRequest.Serie> series) {

        public Ligne(Long id, String codeOfficiel, String nom, String secteur, Long zoneId, String zone,
                     Integer presentes, Integer admis, BigDecimal taux, String organisateur) {
            this(id, codeOfficiel, nom, secteur, zoneId, zone, presentes, admis, taux, organisateur, null, null, null, null, List.of());
        }

        public Ligne(Long id, String codeOfficiel, String nom, String secteur, Long zoneId, String zone,
                     Integer presentes, Integer admis, BigDecimal taux) {
            this(id, codeOfficiel, nom, secteur, zoneId, zone, presentes, admis, taux, null);
        }
    }

    /** Une part des résultats : filles, garçons, une série du baccalauréat. */
    public record Repartition(String libelle, long presentes, long admis, BigDecimal taux) {
    }

    /**
     * Statistiques officielles : absents, éliminés, taux par sexe (sur les établissements qui l'ont renseigné), par région,
     * par préfecture et, pour le baccalauréat, par série. Le taux de réussite est calculé sur les présents.
     */
    public record Statistiques(long absents, long elimines, List<Repartition> parSexe, List<Synthese> parRegion,
                               List<Synthese> parPrefecture, List<Repartition> parSerie) {
    }
}
