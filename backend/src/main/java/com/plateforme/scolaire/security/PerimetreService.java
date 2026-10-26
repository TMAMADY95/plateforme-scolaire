package com.plateforme.scolaire.security;

import com.plateforme.scolaire.domain.Etablissement;
import com.plateforme.scolaire.domain.Perimetre;
import com.plateforme.scolaire.domain.Role;
import com.plateforme.scolaire.domain.ZoneGeographique;
import com.plateforme.scolaire.repository.AffectationEnseignantRepository;
import com.plateforme.scolaire.repository.ClasseRepository;
import com.plateforme.scolaire.repository.EleveRepository;
import com.plateforme.scolaire.repository.EtablissementRepository;
import com.plateforme.scolaire.repository.InscriptionRepository;
import com.plateforme.scolaire.repository.LienParentalRepository;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cœur du contrôle d'accès par périmètre, utilisable dans les annotations :
 * <pre>@PreAuthorize("@perimetre.peutAccederEtablissement(#id)")</pre>
 *
 * <ul>
 *   <li><b>Accéder</b> (lire) : le périmètre de l'utilisateur couvre l'établissement.</li>
 *   <li><b>Gérer</b> (écrire) : direction ou personnel administratif de CET établissement.</li>
 *   <li>Un élève se rattache à un établissement par ses inscriptions en attente ou acceptées.
 *       Un parent ou l'élève lui-même accède à son propre dossier.</li>
 *   <li>L'ADMIN_TECHNIQUE n'a jamais d'accès automatique aux données d'un établissement.</li>
 * </ul>
 */
@Service("perimetre")
@RequiredArgsConstructor
public class PerimetreService {

    private static final Set<Role> ROLES_DE_GESTION =
            EnumSet.of(Role.DIRECTION_ETABLISSEMENT, Role.PERSONNEL_ADMIN);

    private final EtablissementRepository etablissementRepository;
    private final ClasseRepository classeRepository;
    private final InscriptionRepository inscriptionRepository;
    private final EleveRepository eleveRepository;
    private final LienParentalRepository lienParentalRepository;
    private final AffectationEnseignantRepository affectationEnseignantRepository;

    public UtilisateurPrincipal utilisateurCourant() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UtilisateurPrincipal p) {
            return p;
        }
        return null;
    }

    // ---------- Établissements ----------

    @Transactional(readOnly = true)
    public boolean peutAccederEtablissement(Long etablissementId) {
        UtilisateurPrincipal p = utilisateurCourant();
        if (p == null || etablissementId == null) {
            return false;
        }
        return p.getAffectations().stream().anyMatch(a -> couvre(a, etablissementId));
    }

    @Transactional(readOnly = true)
    public boolean peutGererEtablissement(Long etablissementId) {
        UtilisateurPrincipal p = utilisateurCourant();
        if (p == null || etablissementId == null) {
            return false;
        }
        return p.getAffectations().stream().anyMatch(a ->
                ROLES_DE_GESTION.contains(a.role())
                        && a.perimetre() == Perimetre.ETABLISSEMENT
                        && etablissementId.equals(a.etablissementId()));
    }

    // ---------- Classes ----------

    @Transactional(readOnly = true)
    public boolean peutAccederClasse(Long classeId) {
        return etablissementDeClasse(classeId).map(this::peutAccederEtablissement).orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean peutGererClasse(Long classeId) {
        return etablissementDeClasse(classeId).map(this::peutGererEtablissement).orElse(false);
    }

    // ---------- Inscriptions ----------

    @Transactional(readOnly = true)
    public boolean peutAccederInscription(Long inscriptionId) {
        return etablissementDInscription(inscriptionId).map(this::peutAccederEtablissement).orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean peutGererInscription(Long inscriptionId) {
        return etablissementDInscription(inscriptionId).map(this::peutGererEtablissement).orElse(false);
    }

    // ---------- Élèves ----------

    @Transactional(readOnly = true)
    public boolean peutAccederEleve(Long eleveId) {
        UtilisateurPrincipal p = utilisateurCourant();
        if (p == null || eleveId == null) {
            return false;
        }
        boolean parEtablissement = inscriptionRepository.findEtablissementIdsActifsByEleveId(eleveId).stream()
                .anyMatch(this::peutAccederEtablissement);
        if (parEtablissement) {
            return true;
        }
        boolean aUnRolePersonnel = p.getAffectations().stream().anyMatch(a -> a.perimetre() == Perimetre.PERSONNEL);
        return aUnRolePersonnel
                && (lienParentalRepository.existsByEleveIdAndUtilisateurId(eleveId, p.getId())
                || eleveRepository.existsByIdAndUtilisateurId(eleveId, p.getId()));
    }

    @Transactional(readOnly = true)
    public boolean peutGererEleve(Long eleveId) {
        if (utilisateurCourant() == null || eleveId == null) {
            return false;
        }
        return inscriptionRepository.findEtablissementIdsActifsByEleveId(eleveId).stream()
                .anyMatch(this::peutGererEtablissement);
    }

    // ---------- Présences ----------

    /** Saisir l'appel : direction ou personnel administratif de l'établissement, ou enseignant de la classe. */
    @Transactional(readOnly = true)
    public boolean peutSaisirPresences(Long classeId) {
        UtilisateurPrincipal p = utilisateurCourant();
        if (p == null || classeId == null) {
            return false;
        }
        Optional<Long> etablissementId = etablissementDeClasse(classeId);
        if (etablissementId.isEmpty()) {
            return false;
        }
        return peutGererEtablissement(etablissementId.get())
                || affectationEnseignantRepository.existsByClasseIdAndEnseignantUtilisateurId(classeId, p.getId());
    }

    /**
     * Consulter les présences d'une classe : ceux qui peuvent saisir l'appel, plus les responsables
     * nationaux et régionaux. Un enseignant ne voit pas les présences des classes qui ne sont pas les siennes.
     */
    @Transactional(readOnly = true)
    public boolean peutConsulterPresences(Long classeId) {
        UtilisateurPrincipal p = utilisateurCourant();
        if (p == null || classeId == null) {
            return false;
        }
        Optional<Long> etablissementId = etablissementDeClasse(classeId);
        if (etablissementId.isEmpty()) {
            return false;
        }
        if (peutSaisirPresences(classeId)) {
            return true;
        }
        return p.getAffectations().stream().anyMatch(a ->
                (a.perimetre() == Perimetre.NATIONAL || a.perimetre() == Perimetre.ZONE)
                        && couvre(a, etablissementId.get()));
    }

    // ---------- Listes ----------

    @Transactional(readOnly = true)
    public List<Etablissement> etablissementsAccessibles(UtilisateurPrincipal p) {
        Map<Long, Etablissement> resultat = new LinkedHashMap<>();
        for (UtilisateurPrincipal.AffectationInfo a : p.getAffectations()) {
            switch (a.perimetre()) {
                case NATIONAL -> {
                    if (a.role() != Role.ADMIN_TECHNIQUE) {
                        return etablissementRepository.findAll(Sort.by("nom"));
                    }
                }
                case ETABLISSEMENT -> etablissementRepository.findById(a.etablissementId())
                        .ifPresent(e -> resultat.put(e.getId(), e));
                case ZONE -> etablissementRepository.findByZoneEtSousZones(a.zoneId())
                        .forEach(e -> resultat.put(e.getId(), e));
                case PERSONNEL -> {
                }
            }
        }
        return List.copyOf(resultat.values());
    }

    // ---------- Interne ----------

    private Optional<Long> etablissementDeClasse(Long classeId) {
        return classeId == null ? Optional.empty() : classeRepository.findEtablissementIdById(classeId);
    }

    private Optional<Long> etablissementDInscription(Long inscriptionId) {
        return inscriptionId == null ? Optional.empty() : inscriptionRepository.findEtablissementIdById(inscriptionId);
    }

    private boolean couvre(UtilisateurPrincipal.AffectationInfo a, Long etablissementId) {
        return switch (a.perimetre()) {
            case NATIONAL -> a.role() != Role.ADMIN_TECHNIQUE;
            case ETABLISSEMENT -> etablissementId.equals(a.etablissementId());
            case ZONE -> etablissementDansZone(etablissementId, a.zoneId());
            case PERSONNEL -> false;
        };
    }

    private boolean etablissementDansZone(Long etablissementId, Long zoneId) {
        if (zoneId == null) {
            return false;
        }
        return etablissementRepository.findById(etablissementId).map(e -> {
            ZoneGeographique zone = e.getZone();
            while (zone != null) {
                if (zoneId.equals(zone.getId())) {
                    return true;
                }
                zone = zone.getParent();
            }
            return false;
        }).orElse(false);
    }
}
