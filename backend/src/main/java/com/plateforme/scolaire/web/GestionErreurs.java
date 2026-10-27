package com.plateforme.scolaire.web;

import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/**
 * Toutes les erreurs renvoient un corps JSON {"status": ..., "detail": "message lisible"}.
 * Sans cela, Spring masque par défaut le message des erreurs métier.
 */
@RestControllerAdvice
public class GestionErreurs {

    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail erreurMetier(ResponseStatusException e) {
        return ProblemDetail.forStatusAndDetail(e.getStatusCode(), e.getReason());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail donneesInvalides(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(erreur -> erreur.getField() + " : " + erreur.getDefaultMessage())
                .collect(Collectors.joining(" ; "));
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                detail.isEmpty() ? "Données invalides" : detail);
    }

    /** Filet de sécurité : une contrainte de la base (unicité, clé étrangère) a été violée. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail conflit(DataIntegrityViolationException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Conflit avec des données existantes");
    }
}
