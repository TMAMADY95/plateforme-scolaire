package com.plateforme.scolaire.web;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GestionErreurs {

    /** Filet de sécurité : une contrainte de la base (unicité, clé étrangère) a été violée. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail conflit(DataIntegrityViolationException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Conflit avec des données existantes");
    }
}
