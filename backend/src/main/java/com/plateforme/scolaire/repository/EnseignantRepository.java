package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.Enseignant;
import com.plateforme.scolaire.domain.Role;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EnseignantRepository extends JpaRepository<Enseignant, Long> {

    boolean existsByMatricule(String matricule);

    Optional<Enseignant> findByUtilisateurId(Long utilisateurId);

    @Query("select e from Enseignant e where exists (select 1 from AffectationRole a "
            + "where a.utilisateur.id = e.utilisateur.id and a.role = :role "
            + "and a.etablissement.id = :etablissementId) "
            + "order by e.utilisateur.nom, e.utilisateur.prenom")
    List<Enseignant> findByEtablissement(@Param("etablissementId") Long etablissementId, @Param("role") Role role);

    /** Nombre de rattachements de cet enseignant à l'établissement (0 = n'y travaille pas). */
    @Query("select count(a) from Enseignant e, AffectationRole a where e.id = :enseignantId "
            + "and a.utilisateur.id = e.utilisateur.id and a.role = :role "
            + "and a.etablissement.id = :etablissementId")
    long compterRattachements(@Param("enseignantId") Long enseignantId,
                              @Param("etablissementId") Long etablissementId, @Param("role") Role role);
}
