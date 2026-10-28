package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.ZoneGeographique;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ZoneGeographiqueRepository extends JpaRepository<ZoneGeographique, Long> {

    List<ZoneGeographique> findAllByOrderByNom();

    boolean existsByParentId(Long parentId);
}
