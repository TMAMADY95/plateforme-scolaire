package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.PreparationCentre;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PreparationCentreRepository extends JpaRepository<PreparationCentre, Long> {

    List<PreparationCentre> findByCentreId(Long centreId);

    Optional<PreparationCentre> findByCentreIdAndElement(Long centreId, String element);

    @Query("select p from PreparationCentre p where p.centre.examen.id = :examenId")
    List<PreparationCentre> deLExamen(@Param("examenId") Long examenId);
}
