package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.SituationStatistique;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SituationStatistiqueRepository extends JpaRepository<SituationStatistique, Long> {

    @Query("select distinct s from SituationStatistique s left join fetch s.niveaux where s.anneeScolaire.id = :a order by s.etablieLe desc, s.id desc")
    List<SituationStatistique> deLAnnee(@Param("a") Long anneeId);

    boolean existsByAnneeScolaireIdAndStatut(Long anneeId, String statut);
}
