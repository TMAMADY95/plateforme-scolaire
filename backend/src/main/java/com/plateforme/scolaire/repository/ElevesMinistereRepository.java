package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.Inscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Recherche d'inscriptions (donc d'élèves) à l'échelle du pays, avec filtres combinables. */
public interface ElevesMinistereRepository extends JpaRepository<Inscription, Long>, JpaSpecificationExecutor<Inscription> {
}
