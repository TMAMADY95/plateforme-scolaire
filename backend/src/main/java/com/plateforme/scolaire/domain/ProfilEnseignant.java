package com.plateforme.scolaire.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Fiche professionnelle d'un enseignant (une seule par enseignant). */
@Entity
@Table(name = "profil_enseignant")
@Getter
@Setter
@NoArgsConstructor
public class ProfilEnseignant {

    @Id
    @Column(name = "enseignant_id")
    private Long enseignantId;

    @Column(name = "date_naissance")
    private LocalDate dateNaissance;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.CHAR)
    @Column(length = 1)
    private String sexe;

    /** FONCTIONNAIRE, CONTRACTUEL, VOLONTAIRE ou VACATAIRE. */
    @Column(length = 20)
    private String statut;

    /** BAC, BAC_2, LICENCE, MASTER, DOCTORAT ou AUTRE. */
    @Column(name = "formation_academique", length = 20)
    private String formationAcademique;

    /** AUCUNE, ENI (école normale d'instituteurs), ENS (école normale supérieure) ou AUTRE. */
    @Column(name = "formation_professionnelle", length = 20)
    private String formationProfessionnelle;

    @Column(name = "matiere_principale_id")
    private Long matierePrincipaleId;

    @Column(name = "date_prise_service")
    private LocalDate datePriseService;

    @Column(length = 15)
    private String handicap;

    @Column(name = "mis_a_jour_le", nullable = false)
    private Instant misAJourLe = Instant.now();

    @Column(name = "mis_a_jour_par", length = 255)
    private String misAJourPar;
}
