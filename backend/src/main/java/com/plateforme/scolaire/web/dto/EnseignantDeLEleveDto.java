package com.plateforme.scolaire.web.dto;

import java.util.List;

/** Un enseignant de la classe de l'élève, avec les matières qu'il y assure. */
public record EnseignantDeLEleveDto(Long enseignantId, String nom, List<String> matieres) {
}
