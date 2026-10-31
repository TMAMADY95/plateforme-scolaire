package com.plateforme.scolaire.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Code à usage unique envoyé par e-mail lors d'une connexion à double authentification. */
@Entity
@Table(name = "code_verification")
@Getter
@Setter
@NoArgsConstructor
public class CodeVerification {

    @Id
    @Column(name = "utilisateur_id")
    private Long utilisateurId;

    @Column(name = "code_hash", nullable = false)
    private String codeHash;

    @Column(name = "expire_le", nullable = false)
    private Instant expireLe;

    @Column(nullable = false)
    private int essais;
}
