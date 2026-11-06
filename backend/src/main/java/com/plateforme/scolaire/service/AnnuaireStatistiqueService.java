package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.AnneeScolaire;
import com.plateforme.scolaire.domain.Etablissement;
import com.plateforme.scolaire.repository.AnneeScolaireRepository;
import com.plateforme.scolaire.security.PerimetreService;
import com.plateforme.scolaire.security.UtilisateurPrincipal;
import com.plateforme.scolaire.web.dto.AnnuaireDto;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Annuaire statistique calculé à la demande à partir des inscriptions, des classes et des fiches enseignants du périmètre de l'utilisateur.
 * Hypothèses à confirmer : l'âge est l'année de début de l'année scolaire moins l'année de naissance ; un redoublant est un élève
 * déjà inscrit au même niveau l'année scolaire précédente.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnnuaireStatistiqueService {

    private final AnneeScolaireRepository annees;
    private final PerimetreService perimetre;
    private final EntityManager em;

    /** Pur : rapport une décimale ; null si le dénominateur est nul. */
    static Double ratio(int numerateur, int denominateur) {
        if (denominateur <= 0) {
            return null;
        }
        return BigDecimal.valueOf(numerateur).divide(BigDecimal.valueOf(denominateur), 1, RoundingMode.HALF_UP).doubleValue();
    }

    /** Pur : champ CSV entre guillemets si nécessaire (séparateur « ; »). */
    static String champ(Object valeur) {
        String s = valeur == null ? "" : String.valueOf(valeur);
        return s.contains(";") || s.contains("\"") || s.contains("\n") ? "\"" + s.replace("\"", "\"\"") + "\"" : s;
    }

    /** Pur : CSV lisible dans Excel français (BOM, séparateur « ; », virgule décimale). */
    static String csv(AnnuaireDto a) {
        StringBuilder sb = new StringBuilder("﻿");
        sb.append("Annuaire statistique ").append(champ(a.annee())).append('\n');
        sb.append("\nNiveau;Classes;Élèves;Filles;Redoublants;Élèves par classe\n");
        for (AnnuaireDto.Niveau n : a.niveaux()) {
            sb.append(champ(n.niveau())).append(';').append(n.classes()).append(';').append(n.eleves()).append(';').append(n.filles()).append(';')
                    .append(n.redoublants()).append(';').append(decimal(n.elevesParClasse())).append('\n');
        }
        sb.append("\nÂge;Garçons;Filles\n");
        for (AnnuaireDto.Age g : a.ages()) {
            sb.append(g.age()).append(';').append(g.garcons()).append(';').append(g.filles()).append('\n');
        }
        sb.append("\nType d'établissement;Établissements;Élèves\n");
        for (AnnuaireDto.Type t : a.types()) {
            sb.append(champ(t.type())).append(';').append(t.etablissements()).append(';').append(t.eleves()).append('\n');
        }
        AnnuaireDto.Generalites g = a.generalites();
        AnnuaireDto.Enseignants e = a.enseignants();
        sb.append("\nÉtablissements;Classes;Élèves;Filles;Redoublants;Élèves par classe;Enseignants;Enseignantes;Formés;Non titulaires;Élèves par enseignant\n");
        sb.append(g.etablissements()).append(';').append(g.classes()).append(';').append(g.eleves()).append(';').append(g.filles()).append(';')
                .append(g.redoublants()).append(';').append(decimal(g.elevesParClasse())).append(';').append(e.total()).append(';').append(e.femmes()).append(';')
                .append(e.formes()).append(';').append(e.nonTitulaires()).append(';').append(decimal(g.elevesParEnseignant())).append('\n');
        return sb.toString();
    }

    private static String decimal(Double d) {
        return d == null ? "" : String.valueOf(d).replace('.', ',');
    }

    private static int entier(Object o) {
        return o == null ? 0 : ((Number) o).intValue();
    }

    public String csv(Long anneeId) {
        return csv(annuaire(anneeId));
    }

    @SuppressWarnings("unchecked")
    public AnnuaireDto annuaire(Long anneeId) {
        AnneeScolaire annee = annees.findById(anneeId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Année scolaire introuvable"));
        UtilisateurPrincipal p = perimetre.utilisateurCourant();
        if (p == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }
        List<Long> ids = perimetre.etablissementsAccessibles(p).stream().map(Etablissement::getId).toList();
        if (ids.isEmpty()) {
            return new AnnuaireDto(anneeId, annee.getLibelle(), new AnnuaireDto.Generalites(0, 0, 0, 0, 0, null, null), List.of(), List.of(), List.of(),
                    new AnnuaireDto.Enseignants(0, 0, 0, 0, 0));
        }
        Object precedente = em.createNativeQuery("SELECT id FROM annee_scolaire WHERE date_debut < (SELECT date_debut FROM annee_scolaire WHERE id = :a) "
                + "ORDER BY date_debut DESC LIMIT 1").setParameter("a", anneeId).getResultList().stream().findFirst().orElse(null);
        long idPrecedente = precedente == null ? -1L : ((Number) precedente).longValue();

        List<AnnuaireDto.Niveau> niveaux = new ArrayList<>();
        int classes = 0;
        int eleves = 0;
        int filles = 0;
        int redoublants = 0;
        for (Object[] r : (List<Object[]>) em.createNativeQuery(
                "SELECT n.id, n.libelle, count(DISTINCT c.id), count(i.id), count(i.id) FILTER (WHERE e.sexe = 'F'), count(i.id) FILTER (WHERE r.un IS NOT NULL) "
                        + "FROM niveau n JOIN classe c ON c.niveau_id = n.id AND c.annee_scolaire_id = :a AND c.etablissement_id IN (:ids) "
                        + "LEFT JOIN inscription i ON i.classe_id = c.id AND i.statut = 'ACCEPTEE' LEFT JOIN eleve e ON e.id = i.eleve_id "
                        + "LEFT JOIN LATERAL (SELECT 1 AS un FROM inscription p0 JOIN classe pc ON pc.id = p0.classe_id WHERE p0.eleve_id = i.eleve_id "
                        + "AND p0.annee_scolaire_id = :prec AND p0.statut IN ('ACCEPTEE', 'TRANSFEREE') AND pc.niveau_id = c.niveau_id LIMIT 1) r ON TRUE "
                        + "GROUP BY n.id, n.libelle, n.ordre ORDER BY n.ordre")
                .setParameter("a", anneeId).setParameter("ids", ids).setParameter("prec", idPrecedente).getResultList()) {
            int c = entier(r[2]);
            int el = entier(r[3]);
            niveaux.add(new AnnuaireDto.Niveau(((Number) r[0]).longValue(), (String) r[1], c, el, entier(r[4]), entier(r[5]), ratio(el, c)));
            classes += c;
            eleves += el;
            filles += entier(r[4]);
            redoublants += entier(r[5]);
        }

        List<AnnuaireDto.Age> ages = new ArrayList<>();
        for (Object[] r : (List<Object[]>) em.createNativeQuery(
                "SELECT (EXTRACT(YEAR FROM a.date_debut) - EXTRACT(YEAR FROM e.date_naissance))::int AS age, count(*) FILTER (WHERE e.sexe = 'F' ), "
                        + "count(*) FILTER (WHERE e.sexe IS DISTINCT FROM 'F') FROM inscription i JOIN eleve e ON e.id = i.eleve_id "
                        + "JOIN annee_scolaire a ON a.id = i.annee_scolaire_id WHERE i.annee_scolaire_id = :a AND i.statut = 'ACCEPTEE' "
                        + "AND i.etablissement_id IN (:ids) AND e.date_naissance IS NOT NULL GROUP BY age ORDER BY age")
                .setParameter("a", anneeId).setParameter("ids", ids).getResultList()) {
            ages.add(new AnnuaireDto.Age(entier(r[0]), entier(r[2]), entier(r[1])));
        }

        List<AnnuaireDto.Type> types = new ArrayList<>();
        for (Object[] r : (List<Object[]>) em.createNativeQuery(
                "SELECT coalesce(t.libelle, 'Non renseigné'), count(DISTINCT et.id), count(i.id) FROM etablissement et "
                        + "LEFT JOIN type_etablissement t ON t.id = et.type_etablissement_id "
                        + "LEFT JOIN inscription i ON i.etablissement_id = et.id AND i.annee_scolaire_id = :a AND i.statut = 'ACCEPTEE' "
                        + "WHERE et.id IN (:ids) GROUP BY coalesce(t.libelle, 'Non renseigné') ORDER BY 1")
                .setParameter("a", anneeId).setParameter("ids", ids).getResultList()) {
            types.add(new AnnuaireDto.Type((String) r[0], entier(r[1]), entier(r[2])));
        }

        Object[] ens = (Object[]) em.createNativeQuery(
                "SELECT count(DISTINCT e.id), count(DISTINCT e.id) FILTER (WHERE pe.sexe = 'F'), "
                        + "count(DISTINCT e.id) FILTER (WHERE pe.statut IS NOT NULL AND pe.formation_academique IS NOT NULL), "
                        + "count(DISTINCT e.id) FILTER (WHERE pe.formation_professionnelle IS NOT NULL AND pe.formation_professionnelle <> 'AUCUNE'), "
                        + "count(DISTINCT e.id) FILTER (WHERE pe.statut IS NOT NULL AND pe.statut <> 'FONCTIONNAIRE') "
                        + "FROM enseignant e JOIN affectation_role ar ON ar.utilisateur_id = e.utilisateur_id AND ar.role = 'ENSEIGNANT' "
                        + "AND ar.etablissement_id IN (:ids) LEFT JOIN profil_enseignant pe ON pe.enseignant_id = e.id")
                .setParameter("ids", ids).getSingleResult();
        AnnuaireDto.Enseignants enseignants = new AnnuaireDto.Enseignants(entier(ens[0]), entier(ens[1]), entier(ens[2]), entier(ens[3]), entier(ens[4]));
        return new AnnuaireDto(anneeId, annee.getLibelle(),
                new AnnuaireDto.Generalites(ids.size(), classes, eleves, filles, redoublants, ratio(eleves, classes), ratio(eleves, enseignants.total())),
                niveaux, ages, types, enseignants);
    }
}
