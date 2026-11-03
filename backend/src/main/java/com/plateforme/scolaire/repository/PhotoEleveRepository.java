package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.PhotoEleve;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PhotoEleveRepository extends JpaRepository<PhotoEleve, Long> {

    @Query("select p.taille from PhotoEleve p where p.eleveId = :id")
    Optional<Long> tailleDe(@Param("id") Long eleveId);
}
