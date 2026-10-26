package com.plateforme.scolaire.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(name = "etablissement")
@Getter
@Setter
@NoArgsConstructor
public class Etablissement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code_officiel", nullable = false, unique = true, length = 50)
    private String codeOfficiel;

    @Column(nullable = false, length = 200)
    private String nom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id")
    private ZoneGeographique zone;

    @Column(name = "type_etablissement", length = 50)
    private String typeEtablissement;

    @Column(length = 300)
    private String adresse;

    private Integer capacite;

    /** ACTIF, SUSPENDU ou FERME */
    @Column(nullable = false, length = 20)
    private String statut = "ACTIF";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
