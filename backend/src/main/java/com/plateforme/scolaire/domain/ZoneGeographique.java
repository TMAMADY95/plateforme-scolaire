package com.plateforme.scolaire.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@Table(name = "zone_geographique")
@Getter
@Setter
@NoArgsConstructor
public class ZoneGeographique {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private ZoneGeographique parent;

    @Column(nullable = false, length = 150)
    private String nom;

    @Column(name = "type_zone", nullable = false, length = 50)
    private String typeZone;
}
