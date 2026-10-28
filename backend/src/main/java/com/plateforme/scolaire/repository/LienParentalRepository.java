package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.LienParental;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LienParentalRepository extends JpaRepository<LienParental, Long> {

    List<LienParental> findByUtilisateurId(Long utilisateurId);

    List<LienParental> findByEleveId(Long eleveId);

    boolean existsByEleveIdAndUtilisateurId(Long eleveId, Long utilisateurId);

    Optional<LienParental> findByEleveIdAndUtilisateurId(Long eleveId, Long utilisateurId);

    /** Les identifiants des enfants rattachés à un utilisateur (parent). */
    @Query("select l.eleve.id from LienParental l where l.utilisateur.id = :utilisateurId")
    List<Long> findEleveIdsByUtilisateurId(@Param("utilisateurId") Long utilisateurId);
}
