package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.ReinitialisationMotDePasse;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReinitialisationMotDePasseRepository extends JpaRepository<ReinitialisationMotDePasse, Long> {

    Optional<ReinitialisationMotDePasse> findByJetonHash(String jetonHash);

    /** Anti-spam : une demande récente existe déjà pour ce compte. */
    boolean existsByUtilisateurIdAndCreatedAtAfter(Long utilisateurId, Instant depuis);

    /** Supprime les demandes de ce compte (anciennes ou consommées) : un seul lien valide à la fois. */
    @Modifying
    @Query("delete from ReinitialisationMotDePasse r where r.utilisateur.id = :utilisateurId")
    void supprimerDe(@Param("utilisateurId") Long utilisateurId);
}
