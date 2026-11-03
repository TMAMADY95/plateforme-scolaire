package com.plateforme.scolaire.web.dto;

import java.util.List;

/** Résultat du contrôle de cohérence des listes et des notes d'un établissement pour un examen. */
public record ControleCoherenceDto(Long examenId, Long etablissementId, String statutListe, int candidats, int erreurs,
        int avertissements, List<AnomalieExamenDto> anomalies) {}
