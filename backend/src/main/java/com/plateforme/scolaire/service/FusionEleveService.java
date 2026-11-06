package com.plateforme.scolaire.service;

import com.plateforme.scolaire.web.dto.FusionEleveDto;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Fusion de dossiers élèves en double. Tout ce qui référence le dossier absorbé (inscriptions, candidatures aux examens, factures,
 * messages…) est repris par le dossier conservé, puis le dossier absorbé disparaît ; ses identifiants restent valides comme alias.
 * Si les deux dossiers ont des données incompatibles (par exemple une inscription active la même année), la fusion est refusée en entier.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class FusionEleveService {

    private final EntityManager em;
    private final AuditService audit;

    @SuppressWarnings("unchecked")
    @Transactional(readOnly = true)
    public FusionEleveDto doublons() {
        List<Object[]> rows = em.createNativeQuery("SELECT e.id, e.matricule, e.identifiant_national, e.nom, e.prenom, e.date_naissance, e.lieu_naissance, e.nom_mere, "
                + "(SELECT count(*) FROM inscription i WHERE i.eleve_id = e.id), "
                + "(SELECT et.nom FROM inscription i JOIN etablissement et ON et.id = i.etablissement_id WHERE i.eleve_id = e.id ORDER BY i.id DESC LIMIT 1) "
                + "FROM eleve e JOIN (SELECT lower(nom) n, lower(prenom) p, date_naissance d FROM eleve GROUP BY 1, 2, 3 HAVING count(*) > 1 ORDER BY 3, 1, 2 LIMIT 100) g "
                + "ON lower(e.nom) = g.n AND lower(e.prenom) = g.p AND e.date_naissance = g.d ORDER BY e.date_naissance, lower(e.nom), lower(e.prenom), e.id").getResultList();
        Map<String, List<Object[]>> groupes = new LinkedHashMap<>();
        for (Object[] r : rows) {
            groupes.computeIfAbsent(((String) r[3]).toLowerCase() + "|" + ((String) r[4]).toLowerCase() + "|" + r[5], k -> new ArrayList<>()).add(r);
        }
        List<FusionEleveDto.Groupe> res = new ArrayList<>();
        for (List<Object[]> g : groupes.values()) {
            Object[] t = g.get(0);
            res.add(new FusionEleveDto.Groupe((String) t[3], (String) t[4], ((java.sql.Date) t[5]).toLocalDate(),
                    g.stream().map(r -> new FusionEleveDto.Dossier(((Number) r[0]).longValue(), (String) r[1], (String) r[2], (String) r[6], (String) r[7],
                            ((Number) r[8]).intValue(), (String) r[9])).toList()));
        }
        return new FusionEleveDto(res);
    }

    @SuppressWarnings("unchecked")
    public FusionEleveDto.Resultat fusionner(Long conserveId, FusionEleveDto.Requete q, String auteur) {
        Long absorbeId = q.doublonId();
        if (conserveId.equals(absorbeId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le dossier à absorber doit être différent du dossier conservé");
        }
        Object[] conserve = dossier(conserveId);
        Object[] absorbe = dossier(absorbeId);
        if (conserve == null || absorbe == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Dossier élève introuvable");
        }
        if (absorbe[2] != null && conserve[2] != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Les deux dossiers ont un compte de connexion : supprimez l'un des comptes avant de fusionner");
        }

        // Données qui peuvent légitimement exister en double : on garde celles du dossier conservé
        em.createNativeQuery("DELETE FROM photo_eleve WHERE eleve_id = :d AND EXISTS (SELECT 1 FROM photo_eleve WHERE eleve_id = :c)")
                .setParameter("d", absorbeId).setParameter("c", conserveId).executeUpdate();
        em.createNativeQuery("DELETE FROM lien_parental l WHERE l.eleve_id = :d AND EXISTS (SELECT 1 FROM lien_parental p WHERE p.eleve_id = :c AND p.utilisateur_id = l.utilisateur_id)")
                .setParameter("d", absorbeId).setParameter("c", conserveId).executeUpdate();
        if (absorbe[2] != null) {
            em.createNativeQuery("UPDATE eleve SET utilisateur_id = NULL WHERE id = :d").setParameter("d", absorbeId).executeUpdate();
            em.createNativeQuery("UPDATE eleve SET utilisateur_id = :u WHERE id = :c").setParameter("u", absorbe[2]).setParameter("c", conserveId).executeUpdate();
        }

        // Toutes les tables qui référencient eleve(id) : lues dans le catalogue de la base, pour n'en oublier aucune
        List<Object[]> references = em.createNativeQuery("SELECT tc.table_name, kcu.column_name FROM information_schema.table_constraints tc "
                + "JOIN information_schema.key_column_usage kcu ON kcu.constraint_name = tc.constraint_name AND kcu.table_schema = tc.table_schema "
                + "JOIN information_schema.constraint_column_usage ccu ON ccu.constraint_name = tc.constraint_name AND ccu.table_schema = tc.table_schema "
                + "WHERE tc.constraint_type = 'FOREIGN KEY' AND ccu.table_name = 'eleve' AND ccu.column_name = 'id' AND tc.table_schema = current_schema() "
                + "AND tc.table_name NOT IN ('fusion_eleve', 'ancien_identifiant_eleve') ORDER BY 1").getResultList();
        int reprises = 0;
        for (Object[] r : references) {
            String table = (String) r[0];
            String colonne = (String) r[1];
            try {
                reprises += em.createNativeQuery("UPDATE \"" + table + "\" SET \"" + colonne + "\" = :c WHERE \"" + colonne + "\" = :d")
                        .setParameter("c", conserveId).setParameter("d", absorbeId).executeUpdate();
            } catch (RuntimeException e) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Fusion impossible : les deux dossiers ont des données incompatibles (" + table
                        + "), par exemple une inscription active la même année. Résolvez ce conflit puis recommencez.");
            }
        }

        long fusionId = ((Number) em.createNativeQuery("SELECT nextval(pg_get_serial_sequence('fusion_eleve', 'id'))").getSingleResult()).longValue();
        em.createNativeQuery("INSERT INTO fusion_eleve (id, eleve_conserve_id, matricule_absorbe, identifiant_national_absorbe, motif, fait_par) VALUES (:f, :c, :m, :i, :motif, :par)")
                .setParameter("f", fusionId).setParameter("c", conserveId).setParameter("m", absorbe[0]).setParameter("i", absorbe[1])
                .setParameter("motif", q.motif().trim()).setParameter("par", auteur).executeUpdate();
        em.createNativeQuery("DELETE FROM eleve WHERE id = :d").setParameter("d", absorbeId).executeUpdate();
        for (Object ancien : new Object[] {absorbe[0], absorbe[1]}) {
            em.createNativeQuery("INSERT INTO ancien_identifiant_eleve (identifiant, eleve_id, fusion_id) VALUES (:i, :c, :f)")
                    .setParameter("i", ancien).setParameter("c", conserveId).setParameter("f", fusionId).executeUpdate();
        }
        audit.enregistrer("ELEVE_FUSIONNE", "Eleve", conserveId, Map.of("absorbe", String.valueOf(absorbe[1]), "motif", q.motif().trim(), "lignes", reprises));
        return new FusionEleveDto.Resultat(conserveId, (String) conserve[1], (String) absorbe[1], reprises);
    }

    /** [matricule, identifiant national, utilisateur_id] ; null si le dossier n'existe pas. */
    @SuppressWarnings("unchecked")
    private Object[] dossier(Long id) {
        List<Object[]> rows = em.createNativeQuery("SELECT matricule, identifiant_national, utilisateur_id FROM eleve WHERE id = :id").setParameter("id", id).getResultList();
        return rows.isEmpty() ? null : rows.get(0);
    }
}
