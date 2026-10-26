package com.plateforme.scolaire.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Lit le jeton "Authorization: Bearer ...", recharge l'utilisateur en base et refuse le jeton si :
 * le compte est désactivé ou verrouillé, ou si le mot de passe a été changé après l'émission du jeton.
 * Tant qu'un mot de passe initial n'a pas été changé, seuls /api/me et /api/me/mot-de-passe sont accessibles.
 * Instancié dans SecurityConfig : ne pas l'annoter @Component (il serait enregistré deux fois).
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Set<String> CHEMINS_AVANT_CHANGEMENT = Set.of("/api/me", "/api/me/mot-de-passe");

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthFilter(JwtService jwtService, CustomUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            Optional<UtilisateurPrincipal> utilisateur = jwtService.analyser(header.substring(7))
                    .flatMap(jeton -> userDetailsService.chargerParId(jeton.idUtilisateur())
                            .filter(p -> jetonAccepte(jeton, p)));
            if (utilisateur.isPresent()) {
                UtilisateurPrincipal principal = utilisateur.get();
                if (principal.isDoitChangerMotDePasse()
                        && !CHEMINS_AVANT_CHANGEMENT.contains(request.getRequestURI())) {
                    refuserTantQueMotDePasseInitial(response);
                    return;
                }
                var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        chain.doFilter(request, response);
    }

    private static boolean jetonAccepte(JwtService.JetonInfo jeton, UtilisateurPrincipal p) {
        if (!p.isEnabled() || !p.isAccountNonLocked()) {
            return false;
        }
        // Les jetons émis avant le dernier changement de mot de passe sont révoqués.
        return p.getMotDePasseModifieLe() == null || !jeton.emisLe().isBefore(p.getMotDePasseModifieLe());
    }

    private static void refuserTantQueMotDePasseInitial(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"erreur\":\"CHANGEMENT_MOT_DE_PASSE_REQUIS\","
                + "\"message\":\"Vous devez changer votre mot de passe initial (POST /api/me/mot-de-passe).\"}");
    }
}
