package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.Frais;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FraisRepository extends JpaRepository<Frais, Long> {

    List<Frais> findByEtablissementIdAndAnneeScolaireIdOrderByLibelle(Long etablissementId, Long anneeScolaireId);

    /** Frais actifs d'un établissement pour une année, un niveau précis ou tous niveaux. */
    @Query("select f from Frais f where f.etablissement.id = :etablissementId "
            + "and f.anneeScolaire.id = :anneeScolaireId and f.actif = true "
            + "and (f.niveau is null or f.niveau.id = :niveauId) "
            + "order by f.libelle")
    List<Frais> findApplicables(@Param("etablissementId") Long etablissementId,
                                @Param("anneeScolaireId") Long anneeScolaireId,
                                @Param("niveauId") Long niveauId);

    Optional<Frais> findByEtablissementIdAndAnneeScolaireIdAndNiveauIdAndLibelleIgnoreCase(
            Long etablissementId, Long anneeScolaireId, Long niveauId, String libelle);

    Optional<Frais> findByEtablissementIdAndAnneeScolaireIdAndNiveauIsNullAndLibelleIgnoreCase(
            Long etablissementId, Long anneeScolaireId, String libelle);

    /** Sert au contrôle de périmètre. */
    @Query("select f.etablissement.id from Frais f where f.id = :id")
    Optional<Long> findEtablissementIdById(@Param("id") Long id);
}
