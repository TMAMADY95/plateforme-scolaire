package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.Facture;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FactureRepository extends JpaRepository<Facture, Long> {

    List<Facture> findByEleveIdOrderByDateEmissionDescIdDesc(Long eleveId);

    List<Facture> findByEleveIdAndStatutInOrderByDateEmissionDesc(Long eleveId, List<String> statuts);

    List<Facture> findByEtablissementIdAndAnneeScolaireIdOrderByDateEmissionDescIdDesc(
            Long etablissementId, Long anneeScolaireId);

    List<Facture> findByEtablissementIdAndAnneeScolaireIdAndStatutOrderByEleveNomAsc(
            Long etablissementId, Long anneeScolaireId, String statut);

    Optional<Facture> findByFraisIdAndEleveId(Long fraisId, Long eleveId);

    boolean existsByFraisIdAndEleveId(Long fraisId, Long eleveId);

    long countByEtablissementIdAndAnneeScolaireIdAndStatutIn(
            Long etablissementId, Long anneeScolaireId, List<String> statuts);

    /** Sert au contrôle de périmètre. */
    @Query("select f.etablissement.id from Facture f where f.id = :id")
    Optional<Long> findEtablissementIdById(@Param("id") Long id);

    /** Somme des montants dus et des exonérations d'un établissement pour une année. */
    @Query("select coalesce(sum(f.montantDu), 0) as du, coalesce(sum(f.montantExonere), 0) as exonere "
            + "from Facture f where f.etablissement.id = :etablissementId "
            + "and f.anneeScolaire.id = :anneeScolaireId and f.statut <> 'ANNULEE'")
    TotauxFactures totaliser(@Param("etablissementId") Long etablissementId,
                             @Param("anneeScolaireId") Long anneeScolaireId);

    /** Factures impayées ou partielles dont l'échéance est dépassée. */
    @Query("select f from Facture f where f.etablissement.id = :etablissementId "
            + "and f.statut in ('IMPAYEE', 'PARTIELLE') and f.echeance is not null "
            + "and f.echeance < :jour order by f.echeance")
    List<Facture> findEnRetard(@Param("etablissementId") Long etablissementId,
                               @Param("jour") LocalDate jour);

    /** Projection pour les totaux d'un établissement. */
    interface TotauxFactures {
        BigDecimal getDu();

        BigDecimal getExonere();
    }
}
