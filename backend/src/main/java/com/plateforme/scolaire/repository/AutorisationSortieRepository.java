package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.AutorisationSortie;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AutorisationSortieRepository extends JpaRepository<AutorisationSortie, Long> {

    @Query("select s from AutorisationSortie s join fetch s.eleve where s.eleve.id = :eleveId order by s.dateSortie desc, s.id desc")
    List<AutorisationSortie> deLEleve(@Param("eleveId") Long eleveId);

    @Query("select s from AutorisationSortie s join fetch s.eleve where s.etablissement.id = :etablissementId "
            + "and (s.statut = 'DEMANDEE' or s.dateSortie >= :depuis) order by s.dateSortie, s.heureSortie")
    List<AutorisationSortie> deLEtablissement(@Param("etablissementId") Long etablissementId,
                                              @Param("depuis") java.time.LocalDate depuis);
}
