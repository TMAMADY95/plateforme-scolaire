package com.plateforme.scolaire.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Un des sept travaux de préparation matérielle d'un centre d'examen. */
@Entity
@Table(name = "preparation_centre")
@Getter
@Setter
@NoArgsConstructor
public class PreparationCentre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "centre_id")
    private CentreExamen centre;

    @Column(nullable = false, length = 15)
    private String element;

    @Column(nullable = false)
    private boolean fait;

    @Column(length = 500)
    private String observation;

    @Column(name = "fait_le")
    private Instant faitLe;

    @Column(name = "fait_par", length = 255)
    private String faitPar;
}
