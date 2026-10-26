package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.LienParental;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LienParentalRepository extends JpaRepository<LienParental, Long> {
    boolean existsByEleveIdAndUtilisateurId(Long eleveId, Long utilisateurId);
}
