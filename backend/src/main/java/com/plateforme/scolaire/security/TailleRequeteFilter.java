package com.plateforme.scolaire.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Refuse (413) toute requête dont la taille annoncée dépasse la limite, avant de lire le corps. Toutes les
 * requêtes de l'API sont de petits JSON : une limite basse évite qu'un client malveillant fasse charger de gros
 * corps en mémoire. Ne couvre que l'en-tête Content-Length : pour les corps envoyés en morceaux, régler aussi la
 * limite du proxy inverse (voir DEPLOIEMENT.md). Instancié dans SecurityConfig : ne pas l'annoter {@code @Component}.
 */
public class TailleRequeteFilter extends OncePerRequestFilter {

    private final long maximumOctets;

    public TailleRequeteFilter(long maximumOctets) {
        this.maximumOctets = maximumOctets;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (request.getContentLengthLong() > maximumOctets) {
            response.setStatus(413);
            return;
        }
        chain.doFilter(request, response);
    }
}
