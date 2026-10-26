package com.plateforme.scolaire.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    /** Contenu utile d'un jeton valide : à qui il appartient et quand il a été émis. */
    public record JetonInfo(Long idUtilisateur, Instant emisLe) {
    }

    /** La date standard "iat" est à la seconde ; celle-ci, à la milliseconde, sert à la révocation. */
    private static final String REVENDICATION_EMIS_LE_MS = "iatms";

    private final SecretKey cle;
    private final Duration duree;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-minutes}") long minutes) {
        byte[] octets = secret.getBytes(StandardCharsets.UTF_8);
        if (octets.length < 32) {
            throw new IllegalStateException("app.jwt.secret doit contenir au moins 32 caractères");
        }
        this.cle = Keys.hmacShaKeyFor(octets);
        this.duree = Duration.ofMinutes(minutes);
    }

    public String generer(Long idUtilisateur) {
        Instant maintenant = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(idUtilisateur))
                .issuedAt(Date.from(maintenant))
                .claim(REVENDICATION_EMIS_LE_MS, maintenant.toEpochMilli())
                .expiration(Date.from(maintenant.plus(duree)))
                .signWith(cle)
                .compact();
    }

    /** Retourne le contenu du jeton s'il est valide (signature, expiration, date d'émission), sinon vide. */
    public Optional<JetonInfo> analyser(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(cle).build().parseSignedClaims(token).getPayload();
            if (claims.getIssuedAt() == null) {
                return Optional.empty();
            }
            Long emisLeMs = claims.get(REVENDICATION_EMIS_LE_MS, Long.class);
            Instant emisLe = emisLeMs != null ? Instant.ofEpochMilli(emisLeMs) : claims.getIssuedAt().toInstant();
            return Optional.of(new JetonInfo(Long.valueOf(claims.getSubject()), emisLe));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
