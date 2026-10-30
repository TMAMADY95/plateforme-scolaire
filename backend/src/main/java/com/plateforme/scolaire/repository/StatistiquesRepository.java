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

    /** [cycle, nombre d'établissements] ; un établissement à plusieurs cycles compte dans chacun. */
    @Query("select c.libelle, count(distinct e.id) from Etablissement e join e.cycles c "
            + "where e.id in :ids group by c.id, c.libelle, c.ordre order by c.ordre")
    List<Object[]> repartitionParType(@Param("ids") Collection<Long> ids);

    /** [secteur, nombre d'établissements] : PUBLIC puis PRIVE. */
    @Query("select e.secteur, count(e) from Etablissement e where e.id in :ids group by e.secteur order by e.secteur")
    List<Object[]> repartitionParSecteur(@Param("ids") Collection<Long> ids);

    /** Direction d'établissement et personnel administratif (comptes distincts). */
    @Query("select count(distinct a.utilisateur.id) from AffectationRole a "
            + "where a.role in (com.plateforme.scolaire.domain.Role.DIRECTION_ETABLISSEMENT, "
            + "com.plateforme.scolaire.domain.Role.PERSONNEL_ADMIN) and a.etablissement.id in :ids")
    long compterPersonnelDirection(@Param("ids") Collection<Long> ids);

    /** [cycle, effectif] ; les niveaux sans cycle sont regroupés sous « Cycle non défini », en dernier. */
    @Query("select coalesce(cy.libelle, 'Cycle non défini'), count(i) from Inscription i join i.classe c "
            + "join c.niveau n left join n.cycle cy "
            + "where i.etablissement.id in :ids and i.anneeScolaire.id = :annee and i.statut = 'ACCEPTEE' "
            + "group by cy.id, cy.libelle, cy.ordre order by cy.ordre")
    List<Object[]> effectifsParCycle(@Param("ids") Collection<Long> ids, @Param("annee") Long annee);

    /** [sexe (M, F ou null), effectif]. */
    @Query("select e.sexe, count(i) from Inscription i join i.eleve e "
            + "where i.etablissement.id in :ids and i.anneeScolaire.id = :annee and i.statut = 'ACCEPTEE' "
            + "group by e.sexe")
    List<Object[]> effectifsParSexe(@Param("ids") Collection<Long> ids, @Param("annee") Long annee);

    /** [établissement, élèves] (inscriptions acceptées de l'année). */
    @Query("select i.etablissement.id, count(i) from Inscription i "
            + "where i.etablissement.id in :ids and i.anneeScolaire.id = :annee and i.statut = 'ACCEPTEE' "
            + "group by i.etablissement.id")
    List<Object[]> elevesParEtablissement(@Param("ids") Collection<Long> ids, @Param("annee") Long annee);

    /** [établissement, classes] de l'année. */
    @Query("select c.etablissement.id, count(c) from Classe c "
            + "where c.etablissement.id in :ids and c.anneeScolaire.id = :annee group by c.etablissement.id")
    List<Object[]> classesParEtablissement(@Param("ids") Collection<Long> ids, @Param("annee") Long annee);

    /** [établissement, comptes distincts] pour le rôle donné. */
    @Query("select a.etablissement.id, count(distinct a.utilisateur.id) from AffectationRole a "
            + "where a.role = :role and a.etablissement.id in :ids group by a.etablissement.id")
    List<Object[]> personnelParEtablissement(@Param("ids") Collection<Long> ids,
                                             @Param("role") com.plateforme.scolaire.domain.Role role);

    /** [établissement, comptes distincts] de la direction et du personnel administratif. */
    @Query("select a.etablissement.id, count(distinct a.utilisateur.id) from AffectationRole a "
            + "where a.role in (com.plateforme.scolaire.domain.Role.DIRECTION_ETABLISSEMENT, "
            + "com.plateforme.scolaire.domain.Role.PERSONNEL_ADMIN) and a.etablissement.id in :ids "
            + "group by a.etablissement.id")
    List<Object[]> directionParEtablissement(@Param("ids") Collection<Long> ids);

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
