package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.JuryMembre;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JuryMembreRepository extends JpaRepository<JuryMembre, Long> {

    List<JuryMembre> findByExamenIdOrderByOrdre(Long examenId);

    long countByExamenId(Long examenId);
}
