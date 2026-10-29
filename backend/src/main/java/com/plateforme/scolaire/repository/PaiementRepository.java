package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.Paiement;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaiementRepository extends JpaRepository<Paiement, Long> {

    List<Paiement> findByFactureIdOrderByCreatedAtDescIdDesc(Long factureId);

    Optional<Paiement> findByReferenceInterne(String referenceInterne);

    /** Sert à verrouiller la facture AVANT de relire le paiement. */
    @Query("select p.facture.id from Paiement p where p.referenceInterne = :reference")
    Optional<Long> findFactureIdByReferenceInterne(@Param("reference") String reference);

    /** Un paiement en ligne encore ouvert (non expiré) bloque l'annulation et l'exonération. */
    boolean existsByFactureIdAndStatutAndCreatedAtAfter(Long factureId, String statut, Instant depuis);

    /** Le paiement en ligne ouvert (non expiré) le plus récent, pour le reprendre. */
    Optional<Paiement> findFirstByFactureIdAndStatutAndCreatedAtAfterOrderByCreatedAtDesc(
            Long factureId, String statut, Instant depuis);

    boolean existsByFactureIdAndStatut(Long factureId, String statut);

    /** Identifiants des paiements encore ouverts créés avant [avant] (tâche d'expiration). */
    @Query("select p.id from Paiement p where p.statut = 'EN_ATTENTE' and p.createdAt < :avant order by p.id")
    List<Long> idsEnAttenteAvant(@Param("avant") Instant avant);

    /** Total encaissé (null s'il n'y a aucun paiement validé). */
    @Query("select sum(p.montant) from Paiement p where p.facture.id = :factureId and p.statut = 'VALIDE'")
    BigDecimal totalValide(@Param("factureId") Long factureId);

    /** Total des paiements en ligne ouverts et non expirés. */
    @Query("select sum(p.montant) from Paiement p where p.facture.id = :factureId "
            + "and p.statut = 'EN_ATTENTE' and p.createdAt > :depuis")
    BigDecimal totalEnAttente(@Param("factureId") Long factureId, @Param("depuis") Instant depuis);

    /** Totaux encaissés pour plusieurs factures : [factureId, somme]. */
    @Query("select p.facture.id, sum(p.montant) from Paiement p "
            + "where p.facture.id in :ids and p.statut = 'VALIDE' group by p.facture.id")
    List<Object[]> totauxValides(@Param("ids") Collection<Long> ids);
}
