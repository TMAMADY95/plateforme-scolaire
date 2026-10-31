package com.plateforme.scolaire.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Fichier (PDF ou photo) joint à une inspection : preuve de la visite. */
@Entity
@Table(name = "piece_jointe")
@Getter
@Setter
@NoArgsConstructor
public class PieceJointe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inspection_id")
    private Inspection inspection;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ajoutee_par_id")
    private Utilisateur ajouteePar;

    @Column(nullable = false)
    private String nom;

    @Column(name = "type_mime", nullable = false, length = 100)
    private String typeMime;

    @Column(nullable = false)
    private long taille;

    @Basic(fetch = FetchType.LAZY)
    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(nullable = false)
    private byte[] contenu;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
