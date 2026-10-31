package com.plateforme.scolaire.web.dto;

import java.time.LocalDate;

/** Une ligne de la liste nationale des élèves : une inscription de l'année choisie. */
public record EleveListeDto(Long inscriptionId, Long eleveId, String matricule, String nom, String prenom, String sexe,
                            LocalDate dateNaissance, String statut, Long etablissementId, String etablissement,
                            String secteur, String typeEtablissement, String zone, String classe, String niveau,
                            String cycle) {
}
