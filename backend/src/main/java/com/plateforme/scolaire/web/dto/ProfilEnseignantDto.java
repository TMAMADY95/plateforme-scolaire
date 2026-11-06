package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import java.util.List;

/** Profils professionnels des enseignants : fiche individuelle (direction) et synthèse (totaux, sans données nominatives). */
public record ProfilEnseignantDto() {

    /** renseigne : au moins le statut et la formation académique sont connus ; age : en années, null sans date de naissance. */
    public record Profil(Long enseignantId, String nom, String prenom, String matricule, LocalDate dateNaissance, Integer age, String sexe, String statut,
            String formationAcademique, String formationProfessionnelle, Long matierePrincipaleId, String matierePrincipale, LocalDate datePriseService,
            String handicap, boolean renseigne, boolean forme) {
    }

    public record Requete(@Past LocalDate dateNaissance, @Pattern(regexp = "[FM]") String sexe,
            @Pattern(regexp = "FONCTIONNAIRE|CONTRACTUEL|VOLONTAIRE|VACATAIRE") String statut,
            @Pattern(regexp = "BAC|BAC_2|LICENCE|MASTER|DOCTORAT|AUTRE") String formationAcademique,
            @Pattern(regexp = "AUCUNE|ENI|ENS|AUTRE") String formationProfessionnelle, Long matierePrincipaleId, LocalDate datePriseService,
            @Pattern(regexp = "MOTEUR|VISUEL|AUDITIF|INTELLECTUEL|AUTRE") String handicap) {
    }

    /** postes : postes prévus (null si non renseigné) ; deficit : postes moins enseignants, jamais négatif ; null sans postes. */
    public record Ligne(Long etablissementId, String etablissement, String zone, int enseignants, int profilsRenseignes, int formes, int nonTitulaires,
            Integer postes, Integer deficit) {
    }

    public record Resume(int etablissements, int enseignants, int profilsRenseignes, int formes, int nonTitulaires, int femmes, int deficitTotal) {
    }

    public record Synthese(Resume resume, List<Ligne> etablissements) {
    }
}
