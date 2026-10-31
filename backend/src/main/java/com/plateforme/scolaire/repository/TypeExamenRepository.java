package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.TypeExamen;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TypeExamenRepository extends JpaRepository<TypeExamen, Long> {

    List<TypeExamen> findAllByOrderByCode();

    boolean existsByCodeIgnoreCase(String code);
}
