package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.Evaluation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {

    List<Evaluation> findByClasseIdOrderByDateEvaluationDescIdDesc(Long classeId);

    @Query("select e.classe.id from Evaluation e where e.id = :id")
    Optional<Long> findClasseIdById(@Param("id") Long id);
}
