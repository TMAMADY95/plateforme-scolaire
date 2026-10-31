package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.CommissionCorrection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommissionCorrectionRepository extends JpaRepository<CommissionCorrection, Long> {

    @Query("select c from CommissionCorrection c left join fetch c.epreuve e left join fetch e.matiere "
            + "where c.examen.id = :examenId order by case when c.epreuve is null then 0 else 1 end, c.nom")
    List<CommissionCorrection> deLExamen(@Param("examenId") Long examenId);
}
