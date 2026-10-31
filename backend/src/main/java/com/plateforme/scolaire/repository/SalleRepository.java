package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.Salle;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SalleRepository extends JpaRepository<Salle, Long> {

    List<Salle> findByEtablissementIdOrderByNom(Long etablissementId);

    Optional<Salle> findByEtablissementIdAndNom(Long etablissementId, String nom);

    boolean existsByEtablissementIdAndNom(Long etablissementId, String nom);

    /** Lignes {établissement, nombre de salles, capacité totale}. */
    @Query("select s.etablissement.id, count(s), coalesce(sum(s.capacite), 0) from Salle s group by s.etablissement.id")
    List<Object[]> totauxParEtablissement();

    /** Une ligne {nombre de salles, capacité totale} (ou aucune). */
    @Query("select count(s), coalesce(sum(s.capacite), 0) from Salle s where s.etablissement.id = :id")
    List<Object[]> totaux(@Param("id") Long id);
}
