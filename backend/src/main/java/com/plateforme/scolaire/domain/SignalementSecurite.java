package com.plateforme.scolaire.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Un problème de sécurité constaté dans un établissement, ouvert jusqu'à sa résolution. */
@Entity
@Table(name = "signalement_securite")
@Getter
@Setter
@NoArgsConstructor
public class SignalementSecurite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "etablissement_id")
    private Etablissement etablissement;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "signale_par_id")
    private Utilisateur signalePar;

    /** INCENDIE, BATIMENT, EAU_HYGIENE, CLOTURE ou AUTRE */
    @Column(nullable = false, length = 30)
    private String categorie;

    /** FAIBLE, MOYENNE ou ELEVEE */
    @Column(nullable = false, length = 10)
    private String gravite;

    @Column(nullable = false, length = 2000)
    private String description;

    /** OUVERT ou RESOLU */
    @Column(nullable = false, length = 10)
    private String statut = "OUVERT";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "resolu_at")
    private Instant resoluAt;
}
