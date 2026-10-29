package com.plateforme.scolaire.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Limite le nombre d'appels par adresse IP sur les points d'entrée publics (connexion, mot de passe oublié,
 * réinitialisation) : fenêtre fixe, comptage en mémoire. Au-delà : 429 avec l'en-tête Retry-After.
 *
 * <p>Complète le verrouillage par compte (qui protège un compte précis) en freinant les essais en masse depuis une
 * même adresse. Les limites sont larges car une école entière peut partager une seule adresse. Derrière un
 * répartiteur de charge, configurer {@code server.forward-headers-strategy} pour que l'adresse lue soit celle du
 * client. Le comptage est propre à chaque instance du serveur. Instancié dans SecurityConfig : ne pas l'annoter
 * {@code @Component}.</p>
 */
public class LimiteDebitFilter extends OncePerRequestFilter {

    /** Une règle : au plus [maximum] appels par [fenetre] et par adresse. */
    public record Regle(int maximum, Duration fenetre) {
    }

    private record Fenetre(Instant debut, int appels) {
    }

    private static final int PURGE_TOUS_LES = 500;

    private final Map<String, Regle> reglesParChemin;
    private final Clock clock;
    private final Map<String, Fenetre> compteurs = new ConcurrentHashMap<>();
    private final java.util.concurrent.atomic.AtomicInteger appelsDepuisPurge =
            new java.util.concurrent.atomic.AtomicInteger();

    public LimiteDebitFilter(Map<String, Regle> reglesParChemin, Clock clock) {
        this.reglesParChemin = Map.copyOf(reglesParChemin);
        this.clock = clock;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equals(request.getMethod()) || !reglesParChemin.containsKey(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Regle regle = reglesParChemin.get(request.getRequestURI());
        long attente = secondesAAttendre(request.getRequestURI() + "|" + request.getRemoteAddr(), regle);
        if (attente > 0) {
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(attente));
            response.setContentType("application/json");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write("{\"detail\":\"Trop de tentatives. Réessayez dans " + Math.max(1, (attente + 59) / 60)
                    + " minute(s).\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    /** 0 si l'appel est accepté (et compté), sinon le nombre de secondes avant la prochaine fenêtre. */
    long secondesAAttendre(String cle, Regle regle) {
        Instant maintenant = Instant.now(clock);
        purgerSiBesoin(maintenant);
        long[] attente = {0};
        compteurs.compute(cle, (k, ancienne) -> {
            if (ancienne == null || !maintenant.isBefore(ancienne.debut().plus(regle.fenetre()))) {
                return new Fenetre(maintenant, 1);
            }
            if (ancienne.appels() >= regle.maximum()) {
                attente[0] = Math.max(1, Duration.between(maintenant, ancienne.debut().plus(regle.fenetre())).toSeconds());
                return ancienne;
            }
            return new Fenetre(ancienne.debut(), ancienne.appels() + 1);
        });
        return attente[0];
    }

    /** Évite que la table grossisse indéfiniment : retire les fenêtres terminées. */
    private void purgerSiBesoin(Instant maintenant) {
        if (appelsDepuisPurge.incrementAndGet() % PURGE_TOUS_LES != 0) {
            return;
        }
        Duration plusLongue = reglesParChemin.values().stream().map(Regle::fenetre).max(Duration::compareTo)
                .orElse(Duration.ZERO);
        compteurs.entrySet().removeIf(e -> !maintenant.isBefore(e.getValue().debut().plus(plusLongue)));
    }
}
