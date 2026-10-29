package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.CoefficientMatiere;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoefficientMatiereRepository extends JpaRepository<CoefficientMatiere, Long> {

    List<CoefficientMatiere> findByClasseId(Long classeId);
}
