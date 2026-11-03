package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.BesoinPersonnelEpreuve;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BesoinPersonnelEpreuveRepository extends JpaRepository<BesoinPersonnelEpreuve, Long> {

    Optional<BesoinPersonnelEpreuve> findByEpreuveIdAndFonction(Long epreuveId, String fonction);

    @Query("select b from BesoinPersonnelEpreuve b where b.epreuveId in :ids")
    List<BesoinPersonnelEpreuve> desEpreuves(@Param("ids") List<Long> ids);
}
