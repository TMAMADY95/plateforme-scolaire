package com.plateforme.scolaire.web.dto;

import java.util.List;

/** Bilan d'un import, d'une copie ou d'une application du programme. */
public record ProgrammeResultatDto(int lignesCreees, int lignesIgnorees, int matieresCreees, List<String> avertissements) {}
