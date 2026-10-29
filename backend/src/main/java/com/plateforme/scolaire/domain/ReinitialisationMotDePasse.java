package com.plateforme.scolaire.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Demande de réinitialisation du mot de passe. Seule l'empreinte du jeton est conservée. */
@Entity
@Table(name = "reinitialisation_mdp")
@Getter
@Setter
@NoArgsConstructor
public class ReinitialisationMotDePasse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilisateur_id")
    private Utilisateur utilisateur;

    @Column(name = "jeton_hash", nullable = false, length = 64)
    private String jetonHash;

    @Column(name = "expire_le", nullable = false)
    private Instant expireLe;

    @Column(name = "utilise_le")
    private Instant utiliseLe;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
