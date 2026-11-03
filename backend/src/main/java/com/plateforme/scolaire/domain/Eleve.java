package com.plateforme.scolaire.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import java.time.Instant;
import java.time.LocalDate;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "eleve")
@Getter
@Setter
@NoArgsConstructor
public class Eleve {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String matricule;

    /** Identifiant national de l'élève, généré par la base à l'insertion (voir V66) et jamais modifié. */
    @Generated
    @Column(insertable = false, updatable = false, length = 14)
    private String ine;

    @Column(nullable = false, length = 100)
    private String nom;

    @Column(nullable = false, length = 100)
    private String prenom;

    @Column(name = "date_naissance", nullable = false)
    private LocalDate dateNaissance;

    @Column(name = "lieu_naissance", length = 150)
    private String lieuNaissance;

    @Column(name = "nom_pere", length = 150)
    private String nomPere;

    @Column(name = "nom_mere", length = 150)
    private String nomMere;

    /** EXTRAIT, JUGEMENT_SUPPLETIF ou AUCUN ; null = non renseigné. */
    @Column(name = "type_acte_naissance", length = 20)
    private String typeActeNaissance;

    @Column(name = "numero_acte_naissance", length = 60)
    private String numeroActeNaissance;

    /** F ou M */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 1)
    private String sexe;

    /** Compte de connexion de l élève, optionnel. */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id")
    private Utilisateur utilisateur;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
