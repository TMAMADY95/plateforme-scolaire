package com.plateforme.scolaire.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Une vérification publique d'un diplôme : code saisi (tronqué) et résultat. Aucune donnée sur la personne qui vérifie n'est conservée. */
@Entity
@Table(name = "verification_diplome")
@Getter
@Setter
@NoArgsConstructor
public class VerificationDiplome {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code_saisi", nullable = false, length = 20)
    private String codeSaisi;

    /** AUTHENTIQUE, REVOQUE ou INCONNU. */
    @Column(nullable = false, length = 12)
    private String resultat;

    @Column(name = "verifie_le", nullable = false)
    private Instant verifieLe;
}
