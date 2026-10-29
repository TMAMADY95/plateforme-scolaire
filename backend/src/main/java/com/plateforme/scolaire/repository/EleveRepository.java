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

    /** Le dossier élève rattaché à un compte de connexion (rôle ELEVE). */
    Optional<Eleve> findByUtilisateurId(Long utilisateurId);

    boolean existsByIdAndUtilisateurId(Long id, Long utilisateurId);

    /** Comptes d'élèves inscrits (en attente ou accepté) dans l'établissement. */
    @Query("select distinct u from Eleve e join e.utilisateur u where e.id in "
            + "(select i.eleve.id from Inscription i where i.etablissement.id = :etablissementId "
            + "and i.statut in ('EN_ATTENTE', 'ACCEPTEE')) "
            + "and (lower(u.nom) like lower(concat('%', :q, '%')) or lower(u.prenom) like lower(concat('%', :q, '%')) "
            + "or lower(u.email) like lower(concat('%', :q, '%'))) "
            + "order by u.nom, u.prenom")
    List<com.plateforme.scolaire.domain.Utilisateur> rechercherComptesEleves(
            @Param("etablissementId") Long etablissementId, @Param("q") String q);

    /** Détection de doublons : même identité, sans tenir compte de la casse. */
    @Query("select e from Eleve e where lower(e.nom) = lower(:nom) and lower(e.prenom) = lower(:prenom) "
            + "and e.dateNaissance = :dateNaissance")
    List<Eleve> findParIdentite(@Param("nom") String nom, @Param("prenom") String prenom,
                                @Param("dateNaissance") LocalDate dateNaissance);

    /** Élèves dont le nom, le prénom (dans les deux ordres) ou le matricule contient le texte. */
    @Query("select e.id from Eleve e where lower(e.matricule) like lower(concat('%', :q, '%')) "
            + "or lower(concat(e.prenom, ' ', e.nom)) like lower(concat('%', :q, '%')) "
            + "or lower(concat(e.nom, ' ', e.prenom)) like lower(concat('%', :q, '%')) order by e.id")
    List<Long> idsParTexte(@Param("q") String q, org.springframework.data.domain.Pageable limite);

    @Query(value = "select nextval('eleve_matricule_seq')", nativeQuery = true)
    Long prochainNumeroMatricule();
}
