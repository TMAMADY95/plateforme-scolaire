package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.ConvoiExamen;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConvoiExamenRepository extends JpaRepository<ConvoiExamen, Long> {

    @Query("select c from ConvoiExamen c join fetch c.centre where c.examen.id = :examenId order by c.departPrevu, c.id")
    List<ConvoiExamen> deLExamen(@Param("examenId") Long examenId);
}
