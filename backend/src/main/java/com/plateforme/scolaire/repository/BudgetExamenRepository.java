package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.BudgetExamen;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetExamenRepository extends JpaRepository<BudgetExamen, Long> {

    List<BudgetExamen> findByExamenId(Long examenId);

    Optional<BudgetExamen> findByExamenIdAndRubrique(Long examenId, String rubrique);
}
