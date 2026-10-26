package com.plateforme.scolaire.web.dto;

import java.math.BigDecimal;
import java.util.List;

/** effectif = nombre d'élèves classés (ayant au moins une note) dans la classe pour la période. */
public record BulletinDto(EleveDto eleve, Long classeId, String classeNom, Long periodeId, String periode,
                          List<LigneBulletinDto> matieres, BigDecimal moyenneGenerale,
                          BigDecimal moyenneGeneraleClasse, Integer rang, int effectif) {
}
