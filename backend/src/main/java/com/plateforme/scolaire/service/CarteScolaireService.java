package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.Etablissement;
import com.plateforme.scolaire.repository.AnneeScolaireRepository;
import com.plateforme.scolaire.security.PerimetreService;
import com.plateforme.scolaire.security.UtilisateurPrincipal;
import com.plateforme.scolaire.web.dto.CarteScolaireDto;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Carte scolaire : besoins en salles de classe et en enseignants par établissement et par zone. Les normes ne sont pas fixées dans le code :
 * l'utilisateur les saisit (valeurs de départ proposées à titre indicatif, à remplacer par les normes officielles).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CarteScolaireService {

    static final int NORME_MIN = 10;
    static final int NORME_MAX = 200;

    private final AnneeScolaireRepository annees;
    private final PerimetreService perimetre;
    private final EntityManager em;

    /** Pur : nombre d'unités nécessaires pour accueillir les élèves, arrondi à l'entier supérieur. */
    static int necessaire(int eleves, int norme) {
        return eleves <= 0 ? 0 : (eleves + norme - 1) / norme;
    }

    /** Pur : manque = nécessaire moins disponible, jamais négatif. */
    static int manque(int necessaire, int disponible) {
        return Math.max(0, necessaire - disponible);
    }

    private static int entier(Object o) {
        return o == null ? 0 : ((Number) o).intValue();
    }

    @SuppressWarnings("unchecked")
    public CarteScolaireDto calculer(Long anneeId, int normeSalle, int normeEnseignant) {
        if (normeSalle < NORME_MIN || normeSalle > NORME_MAX || normeEnseignant < NORME_MIN || normeEnseignant > NORME_MAX) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Les normes doivent être comprises entre " + NORME_MIN + " et " + NORME_MAX + " élèves");
        }
        if (!annees.existsById(anneeId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Année scolaire introuvable");
        }
        UtilisateurPrincipal u = perimetre.utilisateurCourant();
        if (u == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }
        List<Etablissement> liste = perimetre.etablissementsAccessibles(u);
        List<Long> ids = liste.stream().map(Etablissement::getId).toList();
        if (ids.isEmpty()) {
            return new CarteScolaireDto(anneeId, normeSalle, normeEnseignant, new CarteScolaireDto.Total(0, 0, 0, 0, 0), List.of(), List.of());
        }
        Map<Long, Integer> eleves = new HashMap<>();
        for (Object[] r : (List<Object[]>) em.createNativeQuery("SELECT etablissement_id, count(*) FROM inscription WHERE annee_scolaire_id = :a "
                + "AND statut = 'ACCEPTEE' AND etablissement_id IN (:ids) GROUP BY etablissement_id").setParameter("a", anneeId).setParameter("ids", ids).getResultList()) {
            eleves.put(((Number) r[0]).longValue(), entier(r[1]));
        }
        Map<Long, Integer> salles = new HashMap<>();
        for (Object[] r : (List<Object[]>) em.createNativeQuery("SELECT etablissement_id, salles_dur + salles_provisoires FROM fiche_infrastructure "
                + "WHERE annee_scolaire_id = :a AND etablissement_id IN (:ids)").setParameter("a", anneeId).setParameter("ids", ids).getResultList()) {
            salles.put(((Number) r[0]).longValue(), entier(r[1]));
        }
        Map<Long, Integer> enseignants = new HashMap<>();
        for (Object[] r : (List<Object[]>) em.createNativeQuery("SELECT ar.etablissement_id, count(DISTINCT e.id) FROM enseignant e "
                + "JOIN affectation_role ar ON ar.utilisateur_id = e.utilisateur_id AND ar.role = 'ENSEIGNANT' AND ar.etablissement_id IN (:ids) "
                + "GROUP BY ar.etablissement_id").setParameter("ids", ids).getResultList()) {
            enseignants.put(((Number) r[0]).longValue(), entier(r[1]));
        }
        List<CarteScolaireDto.Ligne> lignes = new ArrayList<>();
        Map<String, int[]> zones = new LinkedHashMap<>();
        int[] total = new int[5];
        for (Etablissement e : liste) {
            int n = eleves.getOrDefault(e.getId(), 0);
            Integer s = salles.get(e.getId());
            int ens = enseignants.getOrDefault(e.getId(), 0);
            int necessairesSalles = necessaire(n, normeSalle);
            int necessairesEns = necessaire(n, normeEnseignant);
            Integer manqueSalles = s == null ? null : manque(necessairesSalles, s);
            int manqueEns = manque(necessairesEns, ens);
            String zone = e.getZone() == null ? "Zone non renseignée" : e.getZone().getNom();
            lignes.add(new CarteScolaireDto.Ligne(e.getId(), e.getNom(), zone, n, s, necessairesSalles, manqueSalles, ens, necessairesEns, manqueEns));
            int[] z = zones.computeIfAbsent(zone, k -> new int[5]);
            int[][] cibles = {z, total};
            for (int[] c : cibles) {
                c[0]++;
                c[1] += n;
                c[2] += manqueSalles == null ? 0 : manqueSalles;
                c[3] += manqueEns;
                c[4] += s == null ? 1 : 0;
            }
        }
        lignes.sort(Comparator.comparing((CarteScolaireDto.Ligne l) -> l.manqueSalles() == null ? 0 : l.manqueSalles()).reversed()
                .thenComparing(Comparator.comparing(CarteScolaireDto.Ligne::manqueEnseignants).reversed()).thenComparing(CarteScolaireDto.Ligne::etablissement));
        List<CarteScolaireDto.Zone> zonesDto = zones.entrySet().stream()
                .map(en -> new CarteScolaireDto.Zone(en.getKey(), en.getValue()[0], en.getValue()[1], en.getValue()[2], en.getValue()[3], en.getValue()[4]))
                .sorted(Comparator.comparing(CarteScolaireDto.Zone::manqueSalles).reversed().thenComparing(CarteScolaireDto.Zone::zone)).toList();
        return new CarteScolaireDto(anneeId, normeSalle, normeEnseignant, new CarteScolaireDto.Total(total[0], total[1], total[2], total[3], total[4]), zonesDto, lignes);
    }
}
