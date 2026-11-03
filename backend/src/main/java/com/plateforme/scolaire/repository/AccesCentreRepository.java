package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.AccesCentre;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccesCentreRepository extends JpaRepository<AccesCentre, Long> {

    boolean existsByUtilisateurIdAndCentreId(Long utilisateurId, Long centreId);

    Optional<AccesCentre> findByUtilisateurIdAndCentreId(Long utilisateurId, Long centreId);

    @Query("select a from AccesCentre a join fetch a.utilisateur where a.centre.id = :centreId order by a.utilisateur.nom")
    List<AccesCentre> duCentre(@Param("centreId") Long centreId);

    @Query("select a from AccesCentre a join fetch a.centre c join fetch c.examen where a.utilisateur.id = :utilisateurId "
            + "order by c.examen.libelle, c.nom")
    List<AccesCentre> deLUtilisateur(@Param("utilisateurId") Long utilisateurId);
}
