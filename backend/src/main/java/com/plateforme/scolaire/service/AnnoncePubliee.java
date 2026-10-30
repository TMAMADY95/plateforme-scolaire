package com.plateforme.scolaire.service;

/** Événement : une annonce vient d'être enregistrée. Porte tout ce qu'il faut pour écrire l'e-mail. */
public record AnnoncePubliee(Long classeId, String classeNom, String titre, String auteur) {
}
