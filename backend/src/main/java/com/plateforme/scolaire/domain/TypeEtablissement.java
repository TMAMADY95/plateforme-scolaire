package com.plateforme.scolaire.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Type d'établissement du référentiel (communautaire, franco-arabe, confessionnel, technique…). */
@Entity
@Table(name = "type_etablissement")
@Getter
@Setter
@NoArgsConstructor
public class TypeEtablissement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(nullable = false, length = 150)
    private String libelle;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
