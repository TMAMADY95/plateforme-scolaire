package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.LienParental;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LienParentalRepository extends JpaRepository<LienParental, Long> {
    boolean existsByEleveIdAndUtilisateurId(Long eleveId, Long utilisateurId);

    List<LienParental> findByUtilisateurId(Long utilisateurId);

    List<LienParental> findByEleveId(Long eleveId);
}
