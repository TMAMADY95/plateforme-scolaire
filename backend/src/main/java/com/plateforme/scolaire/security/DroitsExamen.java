package com.plateforme.scolaire.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Droits sur le circuit des examens, répartis entre les structures administratives :
 * <ul>
 *   <li><b>MINISTERE</b> : suit et consulte le déroulement des examens et valide certaines décisions (la proclamation) ;
 *       il ne saisit rien concernant les examens.</li>
 *   <li><b>DGECS</b> : organise tous les examens (nationaux, régionaux, communaux) : calendrier, types, critères de centres,
 *       centres, sujets, jurys, notes, validation des listes, diplômes.</li>
 *   <li><b>DIRECTION_REGIONALE</b> (IRE) : supervision de sa région.</li>
 *   <li><b>DIRECTION_LOCALE</b> (DPE/DCE) : vérification des listes, centres et personnel de sa préfecture ou commune.</li>
 *   <li><b>CHEF_CENTRE</b> : personnel, incidents et rapports de son centre.</li>
 * </ul>
 */
public final class DroitsExamen {

    /** Lecture du circuit d'examen (ensuite limitée au périmètre de chacun). */
    public static final String CONSULTATION = "hasAnyRole('MINISTERE','DGECS','DIRECTION_REGIONALE','DIRECTION_LOCALE')";

    /** Actions techniques sur le circuit d'examen ; le ministère n'en fait pas partie. */
    public static final String GESTION = "hasAnyRole('DGECS','DIRECTION_REGIONALE','DIRECTION_LOCALE')";

    /** Actions de terrain, ouvertes aussi au chef du centre concerné. */
    public static final String TERRAIN = "hasAnyRole('DGECS','DIRECTION_REGIONALE','DIRECTION_LOCALE','CHEF_CENTRE')";

    /** Lecture de terrain : tout le circuit plus le chef du centre concerné. */
    public static final String LECTURE_TERRAIN =
            "hasAnyRole('MINISTERE','DGECS','DIRECTION_REGIONALE','DIRECTION_LOCALE','CHEF_CENTRE')";

    /** Organisation des examens : DGECS uniquement (le ministère consulte et valide). */
    public static final String NATIONAL = "hasRole('DGECS')";

    /** Décisions validées par le ministère : proclamation des résultats. */
    public static final String MINISTERE = "hasRole('MINISTERE')";

    private DroitsExamen() {
    }

    public static boolean aRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream().anyMatch(a -> ("ROLE_" + role).equals(a.getAuthority()));
    }

    public static boolean estMinistere() {
        return aRole("MINISTERE");
    }

    public static boolean estDgecs() {
        return aRole("DGECS");
    }
}
