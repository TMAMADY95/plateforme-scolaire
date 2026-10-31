package com.plateforme.scolaire.web.dto;

import java.time.LocalDate;
import java.util.List;

/** Diplômes d'un examen : nombre d'admis, de diplômes délivrés et révoqués, et la liste. */
public record DiplomesExamenDto(Long examenId, String statutResultats, long admis, long delivres, long revoques, List<Diplome> diplomes) {

    public record Diplome(Long id, String numero, String titulaire, String etablissement, String mention, LocalDate delivreLe,
                          boolean revoque, String motifRevocation) {
    }
}
