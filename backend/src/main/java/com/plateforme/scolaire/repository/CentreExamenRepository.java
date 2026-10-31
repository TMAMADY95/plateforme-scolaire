package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.CentreExamen;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CentreExamenRepository extends JpaRepository<CentreExamen, Long> {

    @Query("select c from CentreExamen c left join fetch c.zone where c.examen.id = :examenId order by c.nom")
    List<CentreExamen> deLExamen(@Param("examenId") Long examenId);

    boolean existsByExamenIdAndNomIgnoreCase(Long examenId, String nom);
}
