package com.plateforme.scolaire.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Une annonce à toute une classe : informations, sorties, réunions… Sans réponse possible. */
@Entity
@Table(name = "annonce")
@Getter
@Setter
@NoArgsConstructor
public class Annonce {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "classe_id")
    private Classe classe;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auteur_id")
    private Utilisateur auteur;

    @Column(nullable = false, length = 200)
    private String titre;

    @Column(nullable = false, length = 4000)
    private String contenu;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
