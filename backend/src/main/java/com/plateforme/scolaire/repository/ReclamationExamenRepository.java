package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.ReclamationExamen;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReclamationExamenRepository extends JpaRepository<ReclamationExamen, Long> {

    @Query("select r from ReclamationExamen r join fetch r.candidat c join fetch c.eleve where r.examen.id = :examenId order by r.deposeeLe desc, r.id desc")
    List<ReclamationExamen> deLExamen(@Param("examenId") Long examenId);

    @Query("select r from ReclamationExamen r where r.examen.anneeScolaire.id = :anneeId")
    List<ReclamationExamen> deLAnnee(@Param("anneeId") Long anneeId);
}
