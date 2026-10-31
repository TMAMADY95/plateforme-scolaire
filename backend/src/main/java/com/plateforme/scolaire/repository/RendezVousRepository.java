package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.RendezVous;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RendezVousRepository extends JpaRepository<RendezVous, Long> {

    @Query("select r from RendezVous r join fetch r.eleve join fetch r.enseignant en join fetch en.utilisateur "
            + "where r.eleve.id = :eleveId order by r.dateHeure desc")
    List<RendezVous> deLEleve(@Param("eleveId") Long eleveId);

    @Query("select r from RendezVous r join fetch r.eleve join fetch r.enseignant en join fetch en.utilisateur "
            + "where en.utilisateur.id = :utilisateurId and r.statut in ('DEMANDE', 'CONFIRME') order by r.dateHeure")
    List<RendezVous> aVenirDeLEnseignant(@Param("utilisateurId") Long utilisateurId);
}
