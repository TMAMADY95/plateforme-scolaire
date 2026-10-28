package com.plateforme.scolaire.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Une salle d'un établissement (salle de classe, laboratoire, gymnase...). */
@Entity
@Table(name = "salle")
@Getter
@Setter
@NoArgsConstructor
public class Salle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "etablissement_id")
    private Etablissement etablissement;

    @Column(nullable = false, length = 50)
    private String nom;

    private Integer capacite;

    /** SALLE_DE_CLASSE, LABO, GYMNASE... : libre selon le pays. */
    @Column(name = "type_salle", length = 50)
    private String typeSalle;
}
