package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.Size;

/** Pièce reçue ({@code fournie}) ou retirée du dossier ; {@code reference} : numéro d'acte, de diplôme, etc. */
public record PieceCandidatureRequest(boolean fournie, @Size(max = 150) String reference) {
}
