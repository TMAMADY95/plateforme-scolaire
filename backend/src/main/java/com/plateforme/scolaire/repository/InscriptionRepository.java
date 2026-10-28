package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.Inscription;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InscriptionRepository extends JpaRepository<Inscription, Long> {

    List<Inscription> findByEtablissementIdOrderByDateDemandeDescIdDesc(Long etablissementId);

    List<Inscription> findByEtablissementIdAndAnneeScolaireIdOrderByDateDemandeDescIdDesc(
            Long etablissementId, Long anneeScolaireId);

    /** Élèves d'une classe, par ordre alphabétique. */
    List<Inscription> findByClasseIdAndStatutOrderByEleveNomAscElevePrenomAsc(Long classeId, String statut);

    Optional<Inscription> findByEleveIdAndAnneeScolaireIdAndStatutIn(
            Long eleveId, Long anneeScolaireId, Collection<String> statuts);

    long countByClasseIdAndStatut(Long classeId, String statut);

    /** Inscriptions d'un élève dans un statut donné, de l'année la plus récente à la plus ancienne. */
    List<Inscription> findByEleveIdAndStatutOrderByAnneeScolaireDateDebutDesc(Long eleveId, String statut);

    @Query("select i.etablissement.id from Inscription i where i.id = :id")
    Optional<Long> findEtablissementIdById(@Param("id") Long id);

    /** Établissements où l'élève a une inscription en attente ou acceptée. */
    @Query("select i.etablissement.id from Inscription i where i.eleve.id = :eleveId "
            + "and i.statut in ('EN_ATTENTE', 'ACCEPTEE')")
    List<Long> findEtablissementIdsActifsByEleveId(@Param("eleveId") Long eleveId);

    /** Emplois du temps : les inscriptions d'une classe pour une année, avec l'élève chargé. */
    @Query("select i from Inscription i join fetch i.eleve where i.classe.id = :classeId "
            + "and i.statut = :statut")
    List<Inscription> findByClasseIdEtStatutAvecEleve(@Param("classeId") Long classeId,
                                                      @Param("statut") String statut);
}
