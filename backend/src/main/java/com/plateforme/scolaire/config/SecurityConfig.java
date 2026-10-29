package com.plateforme.scolaire.config;

import com.plateforme.scolaire.security.CustomUserDetailsService;
import com.plateforme.scolaire.security.JwtAuthFilter;
import com.plateforme.scolaire.security.JwtService;
import com.plateforme.scolaire.security.LimiteDebitFilter;
import com.plateforme.scolaire.security.TailleRequeteFilter;
import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(CustomUserDetailsService userDetailsService,
                                                PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, JwtService jwtService,
                                    CustomUserDetailsService userDetailsService, Clock clock,
                                    @Value("${app.taille-requete-max:1MB}") org.springframework.util.unit.DataSize tailleMax,
                                    @Value("${app.limite-debit.actif:true}") boolean limiteActive,
                                    @Value("${app.limite-debit.connexion:60}") int limiteConnexion,
                                    @Value("${app.limite-debit.mot-de-passe:10}") int limiteMotDePasse)
            throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // API sans session ni cookie : le jeton est dans l'en-tête
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/error", "/api/sante").permitAll()
                        // Appelé par le prestataire de paiement : protégé par la signature du message, pas par un jeton
                        .requestMatchers("/api/paiements/notification").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .addFilterBefore(new JwtAuthFilter(jwtService, userDetailsService),
                        UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(new TailleRequeteFilter(tailleMax.toBytes()), JwtAuthFilter.class);
        if (limiteActive) {
            http.addFilterBefore(new LimiteDebitFilter(Map.of(
                    "/api/auth/login", new LimiteDebitFilter.Regle(limiteConnexion, Duration.ofMinutes(1)),
                    "/api/auth/mot-de-passe-oublie", new LimiteDebitFilter.Regle(limiteMotDePasse, Duration.ofMinutes(10)),
                    "/api/auth/reinitialisation", new LimiteDebitFilter.Regle(limiteMotDePasse * 2, Duration.ofMinutes(10))),
                    clock), JwtAuthFilter.class);
        }
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.allowed-origins}") String[] origines) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(origines));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
