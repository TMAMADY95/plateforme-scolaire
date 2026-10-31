package com.plateforme.scolaire.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Ce qui a été enseigné pendant une séance : consulté par les élèves et leurs parents. */
@Entity
@Table(name = "cahier_texte")
@Getter
@Setter
@NoArgsConstructor
public class CahierTexte {

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

    @Column(name = "date_seance", nullable = false)
    private LocalDate dateSeance;

    @Column(nullable = false, length = 4000)
    private String contenu;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
