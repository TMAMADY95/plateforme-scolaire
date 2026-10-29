package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.Etablissement;
import java.util.Collection;
import java.util.List;
import org.springframework.data.repository.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Données brutes du taux de réussite, par lots d'établissements (aucune entité chargée). */
public interface ReussiteReseauRepository extends Repository<Etablissement, Long> {

    /** [classeId, établissementId, libellé du niveau, ordre du niveau]. */
    @Query("select c.id, c.etablissement.id, n.libelle, n.ordre from Classe c join c.niveau n "
            + "where c.etablissement.id in :ids and c.anneeScolaire.id = :annee")
    List<Object[]> classes(@Param("ids") Collection<Long> ids, @Param("annee") Long annee);

    /** [classeId, inscriptionId, matiereId, valeur, barème, coefficient] des notes chiffrées de la période. */
    @Query("select e.classe.id, n.inscription.id, e.matiere.id, n.valeur, e.bareme, e.coefficient "
            + "from Note n join n.evaluation e "
            + "where e.classe.etablissement.id in :ids and e.periode.id = :periode "
            + "and n.absent = false and n.valeur is not null")
    List<Object[]> notes(@Param("ids") Collection<Long> ids, @Param("periode") Long periode);

    /** [classeId, matiereId, coefficient] des coefficients de matières définis. */
    @Query("select c.classe.id, c.matiere.id, c.coefficient from CoefficientMatiere c "
            + "where c.classe.etablissement.id in :ids")
    List<Object[]> coefficients(@Param("ids") Collection<Long> ids);
}
