package com.plateforme.scolaire.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Un devoir à faire pour une classe, dans une matière, à rendre pour une date. */
@Entity
@Table(name = "devoir")
@Getter
@Setter
@NoArgsConstructor
public class Devoir {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "classe_id")
    private Classe classe;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "matiere_id")
    private Matiere matiere;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enseignant_id")
    private Enseignant enseignant;

    @Column(nullable = false, length = 200)
    private String titre;

    @Column(length = 4000)
    private String consigne;

    @Column(name = "date_limite", nullable = false)
    private LocalDate dateLimite;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
