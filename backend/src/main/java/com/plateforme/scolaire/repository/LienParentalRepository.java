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

    /** Comptes de parents dont un enfant est inscrit (en attente ou accepté) dans l'établissement. */
    @Query("select distinct u from LienParental l join l.utilisateur u where l.eleve.id in "
            + "(select i.eleve.id from Inscription i where i.etablissement.id = :etablissementId "
            + "and i.statut in ('EN_ATTENTE', 'ACCEPTEE')) "
            + "and (lower(u.nom) like lower(concat('%', :q, '%')) or lower(u.prenom) like lower(concat('%', :q, '%')) "
            + "or lower(u.email) like lower(concat('%', :q, '%'))) "
            + "order by u.nom, u.prenom")
    List<com.plateforme.scolaire.domain.Utilisateur> rechercherParents(
            @Param("etablissementId") Long etablissementId, @Param("q") String q);

    /** Adresses des parents (comptes actifs) dont un enfant est inscrit, inscription acceptée, dans cette classe. */
    @Query("select distinct u.email from LienParental l join l.utilisateur u where u.actif = true "
            + "and u.email is not null and l.eleve.id in "
            + "(select i.eleve.id from Inscription i where i.classe.id = :classeId and i.statut = 'ACCEPTEE')")
    List<String> emailsParentsDeLaClasse(@Param("classeId") Long classeId);

    /** Les identifiants des enfants rattachés à un utilisateur (parent). */
    @Query("select l.eleve.id from LienParental l where l.utilisateur.id = :utilisateurId")
    List<Long> findEleveIdsByUtilisateurId(@Param("utilisateurId") Long utilisateurId);
}
