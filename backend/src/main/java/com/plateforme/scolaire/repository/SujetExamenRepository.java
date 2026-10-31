package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.SujetExamen;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SujetExamenRepository extends JpaRepository<SujetExamen, Long> {

    @Query("select s from SujetExamen s join fetch s.epreuve e where e.examen.id = :examenId order by s.variante")
    List<SujetExamen> deLExamen(@Param("examenId") Long examenId);

    List<SujetExamen> findByEpreuveIdOrderByVariante(Long epreuveId);
}
