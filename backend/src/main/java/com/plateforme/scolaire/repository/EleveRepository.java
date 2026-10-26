package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.Eleve;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EleveRepository extends JpaRepository<Eleve, Long> {

    Optional<Eleve> findByMatricule(String matricule);

    boolean existsByIdAndUtilisateurId(Long id, Long utilisateurId);

    /** Détection de doublons : même identité, sans tenir compte de la casse. */
    @Query("select e from Eleve e where lower(e.nom) = lower(:nom) and lower(e.prenom) = lower(:prenom) "
            + "and e.dateNaissance = :dateNaissance")
    List<Eleve> findParIdentite(@Param("nom") String nom, @Param("prenom") String prenom,
                                @Param("dateNaissance") LocalDate dateNaissance);

    @Query(value = "select nextval('eleve_matricule_seq')", nativeQuery = true)
    Long prochainNumeroMatricule();
}
