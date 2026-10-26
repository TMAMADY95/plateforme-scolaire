package com.plateforme.scolaire.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(name = "utilisateur")
@Getter
@Setter
@NoArgsConstructor
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(name = "mot_de_passe_hash", nullable = false)
    private String motDePasseHash;

    @Column(nullable = false, length = 100)
    private String nom;

    @Column(nullable = false, length = 100)
    private String prenom;

    @Column(length = 30)
    private String telephone;

    @Column(nullable = false)
    private boolean actif = true;

    /** Vrai pour un compte créé avec un mot de passe initial : seul le changement de mot de passe est permis. */
    @Column(name = "doit_changer_mot_de_passe", nullable = false)
    private boolean doitChangerMotDePasse;

    /** Les jetons émis avant cette date sont refusés. */
    @Column(name = "mot_de_passe_modifie_le")
    private Instant motDePasseModifieLe;

    @Column(name = "echecs_connexion", nullable = false)
    private int echecsConnexion;

    @Column(name = "verrouille_jusqu_a")
    private Instant verrouilleJusquA;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
