package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.Etablissement;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.repository.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Agrégats des tableaux de bord. Toutes les requêtes sont limitées à une liste d'établissements. */
public interface StatistiquesRepository extends Repository<Etablissement, Long> {

    @Query("select count(distinct a.utilisateur.id) from AffectationRole a "
            + "where a.role = com.plateforme.scolaire.domain.Role.ENSEIGNANT and a.etablissement.id in :ids")
    long compterEnseignants(@Param("ids") Collection<Long> ids);

    /** [type, nombre d'établissements] ; le type manquant est regroupé sous « Non précisé ». */
    @Query("select coalesce(e.typeEtablissement, 'Non précisé'), count(e) from Etablissement e "
            + "where e.id in :ids group by coalesce(e.typeEtablissement, 'Non précisé') order by count(e) desc")
    List<Object[]> repartitionParType(@Param("ids") Collection<Long> ids);

    /** [niveau, effectif] dans l'ordre des niveaux ; seules les inscriptions acceptées et placées en classe comptent. */
    @Query("select n.libelle, count(i) from Inscription i join i.classe c join c.niveau n "
            + "where i.etablissement.id in :ids and i.anneeScolaire.id = :annee and i.statut = 'ACCEPTEE' "
            + "group by n.id, n.libelle, n.ordre order by n.ordre")
    List<Object[]> effectifsParNiveau(@Param("ids") Collection<Long> ids, @Param("annee") Long annee);

    /** [zone, effectif] des zones les plus peuplées d'abord. */
    @Query("select z.nom, count(i) from Inscription i join i.etablissement e join e.zone z "
            + "where e.id in :ids and i.anneeScolaire.id = :annee and i.statut = 'ACCEPTEE' "
            + "group by z.id, z.nom order by count(i) desc, z.nom")
    List<Object[]> effectifsParZone(@Param("ids") Collection<Long> ids, @Param("annee") Long annee);

    /** [année, mois, nombre] des demandes en attente ou acceptées. */
    @Query("select year(i.dateDemande), month(i.dateDemande), count(i) from Inscription i "
            + "where i.etablissement.id in :ids and i.anneeScolaire.id = :annee "
            + "and i.statut in ('EN_ATTENTE', 'ACCEPTEE') "
            + "group by year(i.dateDemande), month(i.dateDemande) order by year(i.dateDemande), month(i.dateDemande)")
    List<Object[]> inscriptionsParMois(@Param("ids") Collection<Long> ids, @Param("annee") Long annee);

    /** [statut, nombre] des présences saisies depuis la date donnée. */
    @Query("select p.statut, count(p) from Presence p join p.inscription i "
            + "where i.etablissement.id in :ids and p.dateSeance >= :depuis group by p.statut")
    List<Object[]> presencesDepuis(@Param("ids") Collection<Long> ids, @Param("depuis") LocalDate depuis);
}
