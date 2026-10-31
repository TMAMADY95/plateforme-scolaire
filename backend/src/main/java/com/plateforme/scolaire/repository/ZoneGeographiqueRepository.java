package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.ZoneGeographique;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ZoneGeographiqueRepository extends JpaRepository<ZoneGeographique, Long> {

    List<ZoneGeographique> findAllByOrderByNom();

    boolean existsByParentId(Long parentId);

    @Query("select distinct z.typeZone from ZoneGeographique z order by z.typeZone")
    List<String> typesDeZone();
}
