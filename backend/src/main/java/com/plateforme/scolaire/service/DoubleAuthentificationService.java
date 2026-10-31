package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.CodeVerification;
import com.plateforme.scolaire.domain.Role;
import com.plateforme.scolaire.repository.CodeVerificationRepository;
import com.plateforme.scolaire.security.UtilisateurPrincipal;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Double authentification par e-mail pour les comptes qui voient tout un pays ou une région (ministère, directions
 * régionales). Après un mot de passe correct, un code à 6 chiffres valable 10 minutes est envoyé ; 5 essais au plus.
 */
@Service
@Transactional
public class DoubleAuthentificationService {

    static final Duration VALIDITE = Duration.ofMinutes(10);
    static final int ESSAIS_MAX = 5;

    private final CodeVerificationRepository codes;
    private final PasswordEncoder encodeur;
    private final EnvoyeurMail mail;
    private final Clock clock;
    private final boolean active;
    private final SecureRandom hasard = new SecureRandom();

    public DoubleAuthentificationService(CodeVerificationRepository codes, PasswordEncoder encodeur, EnvoyeurMail mail,
                                         Clock clock,
                                         @Value("${app.securite.double-authentification:false}") boolean active) {
        this.codes = codes;
        this.encodeur = encodeur;
        this.mail = mail;
        this.clock = clock;
        this.active = active;
    }

    public boolean requise(UtilisateurPrincipal p) {
        return active && p.getAffectations().stream()
                .anyMatch(a -> a.role() == Role.MINISTERE || a.role() == Role.DIRECTION_REGIONALE);
    }

    public void envoyerCode(Long utilisateurId, String email) {
        String code = String.format("%06d", hasard.nextInt(1_000_000));
        CodeVerification c = codes.findById(utilisateurId).orElseGet(CodeVerification::new);
        c.setUtilisateurId(utilisateurId);
        c.setCodeHash(encodeur.encode(code));
        c.setExpireLe(Instant.now(clock).plus(VALIDITE));
        c.setEssais(0);
        codes.save(c);
        mail.envoyer(email, "Votre code de connexion",
                "Votre code de connexion : " + code + "\n\nIl est valable 10 minutes. "
                        + "Si vous n'êtes pas à l'origine de cette connexion, changez votre mot de passe.\n");
    }

    /** Vrai si le code est le bon ; il ne sert qu'une fois. */
    public boolean verifier(Long utilisateurId, String code) {
        CodeVerification c = codes.findById(utilisateurId).orElse(null);
        if (c == null || code == null) {
            return false;
        }
        if (c.getExpireLe().isBefore(Instant.now(clock)) || c.getEssais() >= ESSAIS_MAX) {
            codes.delete(c);
            return false;
        }
        if (encodeur.matches(code.trim(), c.getCodeHash())) {
            codes.delete(c);
            return true;
        }
        c.setEssais(c.getEssais() + 1);
        return false;
    }
}
