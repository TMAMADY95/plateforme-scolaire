package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.Eleve;
import com.plateforme.scolaire.repository.EleveRepository;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Identifiant national de l'élève : GN-AA-NNNNNNN-CC. AA est l'année d'enregistrement, NNNNNNN un numéro national, CC une clé de contrôle
 * telle que (AANNNNNNN × 100 + CC) est divisible par 97. L'attribution est faite par la base ; ici, la vérification et la recherche.
 */
public final class IdentifiantNational {

    private static final Pattern FORMAT = Pattern.compile("^GN-(\\d{2})-(\\d{7})-(\\d{2})$");

    private IdentifiantNational() {
    }

    /** Pur : clé de contrôle (1 à 97) pour une année sur 2 chiffres et un numéro. */
    static int cle(int annee, long numero) {
        long base = annee * 10_000_000L + numero;
        return (int) (97 - (base * 100) % 97);
    }

    /** Pur : construit l'identifiant. */
    static String generer(int annee, long numero) {
        return String.format("GN-%02d-%07d-%02d", annee, numero, cle(annee, numero));
    }

    /** Pur : forme correcte et clé de contrôle exacte. */
    public static boolean valide(String identifiant) {
        if (identifiant == null) {
            return false;
        }
        Matcher m = FORMAT.matcher(identifiant);
        if (!m.matches()) {
            return false;
        }
        int annee = Integer.parseInt(m.group(1));
        long numero = Long.parseLong(m.group(2));
        return cle(annee, numero) == Integer.parseInt(m.group(3));
    }

    /**
     * Retrouve un élève à partir de ce que l'utilisateur a saisi : matricule (ELV-…), identifiant national (GN-…), ou ancien identifiant
     * d'un dossier fusionné. Un identifiant national mal saisi est refusé avec un message explicite plutôt que traité comme inconnu.
     */
    public static Optional<Eleve> trouver(EleveRepository eleves, String saisie) {
        String s = saisie == null ? "" : saisie.trim();
        Optional<Eleve> parMatricule = eleves.findByMatricule(s);
        if (parMatricule != null && parMatricule.isPresent()) {
            return parMatricule;
        }
        String majuscules = s.toUpperCase(java.util.Locale.ROOT);
        if (majuscules.startsWith("GN-")) {
            if (!valide(majuscules)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Identifiant national invalide : vérifiez la saisie (clé de contrôle incorrecte)");
            }
            Optional<Eleve> national = eleves.findByIdentifiantNational(majuscules);
            if (national != null && national.isPresent()) {
                return national;
            }
        }
        Optional<Eleve> ancien = eleves.findParAncienIdentifiant(majuscules.startsWith("GN-") ? majuscules : s);
        return ancien == null ? Optional.empty() : ancien;
    }
}
