package com.plateforme.scolaire.web.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Dossiers de candidature des candidats d'un établissement. {@code evaluationsManquees} : évaluations de l'année où l'élève
 * a été noté absent (le règlement demande d'avoir participé à toutes les évaluations) ; c'est une alerte, pas un blocage.
 */
public record DossiersCandidaturesDto(Long examenId, String examen, List<TypePiece> pieces, List<Candidat> candidats,
                                      int complets) {

    public record TypePiece(String type, String libelle, boolean requise) {
    }

    public record Piece(String reference, LocalDate fournieLe) {
    }

    public record Candidat(Long id, String nom, String numero, String classe, Map<String, Piece> pieces, boolean complet,
                           List<String> manques, int evaluationsManquees, boolean libre) {
    }
}
