package com.plateforme.scolaire.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Une personne mobilisée au niveau national pour un examen : membre de jury, superviseur, inspecteur ou agent informatique. */
@Entity
@Table(name = "personnel_examen")
@Getter
@Setter
@NoArgsConstructor
public class PersonnelExamen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "examen_id")
    private ExamenNational examen;

    @Column(nullable = false, length = 15)
    private String fonction;

    @Column(nullable = false, length = 150)
    private String nom;

    @Column(length = 30)
    private String telephone;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
