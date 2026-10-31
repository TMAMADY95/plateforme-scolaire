package com.plateforme.scolaire.web.dto;

import java.time.LocalDate;

/**
 * Résultat de la vérification publique d'un diplôme. {@code statut} : AUTHENTIQUE, REVOQUE ou INCONNU ; les autres champs sont
 * vides quand le code est inconnu.
 */
public record VerificationDiplomeDto(String statut, String titulaire, String examen, String anneeScolaire, String mention,
                                     String numero, LocalDate delivreLe, String motifRevocation) {

    public static VerificationDiplomeDto inconnu() {
        return new VerificationDiplomeDto("INCONNU", null, null, null, null, null, null, null);
    }
}
