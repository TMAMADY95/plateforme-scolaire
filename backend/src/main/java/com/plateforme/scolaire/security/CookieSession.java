package com.plateforme.scolaire.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Cookie de session du site web : le jeton y est inaccessible à JavaScript (HttpOnly). Sans durée fixée, il disparaît à la
 * fermeture du navigateur ; le jeton qu'il contient expire de toute façon. L'application mobile n'en utilise pas : elle
 * envoie son jeton dans l'en-tête Authorization.
 */
@Component
public class CookieSession {

    public static final String NOM = "session_scolaire";

    /**
     * En-tête exigé sur les écritures authentifiées par cookie. Un site tiers ne peut pas l'ajouter sans l'accord CORS du
     * serveur, alors que le navigateur peut joindre le cookie de lui-même (protection contre les requêtes forgées).
     */
    public static final String ENTETE_APPLICATION = "X-Requested-With";

    private final boolean secure;

    public CookieSession(@Value("${app.cookie.secure}") boolean secure) {
        this.secure = secure;
    }

    public ResponseCookie poser(String jeton) {
        return base(jeton).build();
    }

    public ResponseCookie effacer() {
        return base("").maxAge(0).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String valeur) {
        return ResponseCookie.from(NOM, valeur).httpOnly(true).secure(secure).sameSite("Strict").path("/api");
    }
}
