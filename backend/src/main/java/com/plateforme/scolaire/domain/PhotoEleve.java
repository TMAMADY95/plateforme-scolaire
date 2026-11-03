package com.plateforme.scolaire.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Photo d'identité d'un élève (une seule par élève). */
@Entity
@Table(name = "photo_eleve")
@Getter
@Setter
@NoArgsConstructor
public class PhotoEleve {

    @Id
    @Column(name = "eleve_id")
    private Long eleveId;

    @Column(name = "type_mime", nullable = false, length = 100)
    private String typeMime;

    @Column(nullable = false)
    private long taille;

    @Basic(fetch = FetchType.LAZY)
    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(nullable = false)
    private byte[] contenu;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "ajoutee_par_id")
    private Long ajouteeParId;
}
