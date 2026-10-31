package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.ExamenNational;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExamenNationalRepository extends JpaRepository<ExamenNational, Long> {

    List<ExamenNational> findByAnneeScolaireIdOrderByLibelle(Long anneeScolaireId);

    boolean existsByLibelleIgnoreCaseAndAnneeScolaireId(String libelle, Long anneeScolaireId);

    @Query("select count(r) from ResultatExamen r where r.examen.id = :id")
    long nombreResultats(@Param("id") Long id);

    @Query("select count(c) from CandidatExamen c where c.examen.id = :id")
    long nombreCandidats(@Param("id") Long id);
}
