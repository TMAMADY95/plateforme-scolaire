package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.ReinitialisationMotDePasse;
import com.plateforme.scolaire.domain.Utilisateur;
import com.plateforme.scolaire.repository.ReinitialisationMotDePasseRepository;
import com.plateforme.scolaire.repository.UtilisateurRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * « Mot de passe oublié » : un lien secret à usage unique, valable 30 minutes, envoyé par e-mail.
 *
 * <p>La demande répond toujours pareil, que l'adresse existe ou non (on ne révèle pas qui a un compte).
 * Le jeton n'est jamais stocké en clair, et le nouveau mot de passe révoque les sessions existantes
 * (motDePasseModifieLe) et lève un éventuel verrouillage.</p>
 */
@Slf4j
@Service
@Transactional
public class ReinitialisationMotDePasseService {

    static final Duration VALIDITE = Duration.ofMinutes(30);
    static final Duration DELAI_ENTRE_DEMANDES = Duration.ofMinutes(2);
    private static final SecureRandom ALEATOIRE = new SecureRandom();

    private final UtilisateurRepository utilisateurRepository;
    private final ReinitialisationMotDePasseRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final EnvoyeurMail envoyeurMail;
    private final AuditService auditService;
    private final Clock clock;
    private final String urlFront;

    public ReinitialisationMotDePasseService(UtilisateurRepository utilisateurRepository,
                                             ReinitialisationMotDePasseRepository repository,
                                             PasswordEncoder passwordEncoder, EnvoyeurMail envoyeurMail,
                                             AuditService auditService, Clock clock,
                                             @Value("${app.mail.url-front:http://localhost:5173}") String urlFront) {
        this.utilisateurRepository = utilisateurRepository;
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.envoyeurMail = envoyeurMail;
        this.auditService = auditService;
        this.clock = clock;
        this.urlFront = urlFront.endsWith("/") ? urlFront.substring(0, urlFront.length() - 1) : urlFront;
    }

    /** Ne signale jamais si l'adresse existe : même comportement visible dans tous les cas. */
    public void demander(String email) {
        Utilisateur utilisateur = utilisateurRepository.findByEmailIgnoreCase(email.trim()).orElse(null);
        if (utilisateur == null || !utilisateur.isActif()) {
            return;
        }
        Instant maintenant = Instant.now(clock);
        if (repository.existsByUtilisateurIdAndCreatedAtAfter(utilisateur.getId(), maintenant.minus(DELAI_ENTRE_DEMANDES))) {
            return;
        }

        String jeton = genererJeton();
        repository.supprimerDe(utilisateur.getId());
        ReinitialisationMotDePasse demande = new ReinitialisationMotDePasse();
        demande.setUtilisateur(utilisateur);
        demande.setJetonHash(empreinte(jeton));
        demande.setExpireLe(maintenant.plus(VALIDITE));
        demande.setCreatedAt(maintenant);
        repository.save(demande);
        auditService.enregistrer("DEMANDE_REINITIALISATION_MDP", "utilisateur", utilisateur.getId(), Map.of());

        try {
            envoyeurMail.envoyer(utilisateur.getEmail(), "Réinitialisation de votre mot de passe",
                    "Bonjour " + utilisateur.getPrenom() + ",\n\n"
                            + "Vous avez demandé à réinitialiser votre mot de passe. Ouvrez ce lien pour en choisir un nouveau "
                            + "(valable " + VALIDITE.toMinutes() + " minutes, une seule fois) :\n\n"
                            + urlFront + "/reinitialisation?jeton=" + jeton + "\n\n"
                            + "Si vous n'êtes pas à l'origine de cette demande, ignorez ce message : votre mot de passe "
                            + "reste inchangé.\n");
        } catch (RuntimeException e) {
            // Une panne d'e-mail ne doit pas révéler que le compte existe : on l'enregistre côté serveur seulement.
            log.error("Envoi de l'e-mail de réinitialisation impossible", e);
        }
    }

    @Transactional(noRollbackFor = ResponseStatusException.class)
    public void reinitialiser(String jeton, String nouveauMotDePasse) {
        Instant maintenant = Instant.now(clock);
        ReinitialisationMotDePasse demande = repository.findByJetonHash(empreinte(jeton.trim()))
                .filter(d -> d.getUtiliseLe() == null && d.getExpireLe().isAfter(maintenant))
                .orElseThrow(ReinitialisationMotDePasseService::lienInvalide);
        Utilisateur utilisateur = demande.getUtilisateur();
        if (!utilisateur.isActif()) {
            throw lienInvalide();
        }

        utilisateur.setMotDePasseHash(passwordEncoder.encode(nouveauMotDePasse));
        utilisateur.setDoitChangerMotDePasse(false);
        utilisateur.setMotDePasseModifieLe(maintenant);
        utilisateur.setEchecsConnexion(0);
        utilisateur.setVerrouilleJusquA(null);
        demande.setUtiliseLe(maintenant);
        auditService.enregistrer("MOT_DE_PASSE_REINITIALISE_PAR_EMAIL", "utilisateur", utilisateur.getId(), Map.of());
    }

    private static ResponseStatusException lienInvalide() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Ce lien est invalide ou a expiré. Refaites une demande de réinitialisation.");
    }

    static String genererJeton() {
        byte[] octets = new byte[32];
        ALEATOIRE.nextBytes(octets);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(octets);
    }

    static String empreinte(String jeton) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(jeton.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }
}
