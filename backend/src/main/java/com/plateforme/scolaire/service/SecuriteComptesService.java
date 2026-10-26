package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.Utilisateur;
import com.plateforme.scolaire.repository.UtilisateurRepository;
import com.plateforme.scolaire.web.dto.ChangementMotDePasseRequest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Verrouillage après échecs de connexion et changement de mot de passe. */
@Service
@Transactional
public class SecuriteComptesService {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final int maxEchecs;
    private final Duration dureeVerrouillage;

    public SecuriteComptesService(UtilisateurRepository utilisateurRepository, PasswordEncoder passwordEncoder,
                                  Clock clock,
                                  @Value("${app.securite.max-echecs:5}") int maxEchecs,
                                  @Value("${app.securite.duree-verrouillage-minutes:15}") long dureeMinutes) {
        this.utilisateurRepository = utilisateurRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.maxEchecs = maxEchecs;
        this.dureeVerrouillage = Duration.ofMinutes(dureeMinutes);
    }

    /** Un échec de connexion pour cette adresse : sans effet si elle n'existe pas. */
    public void enregistrerEchec(String email) {
        utilisateurRepository.findByEmailIgnoreCase(email.trim()).ifPresent(this::compterEchec);
    }

    public void enregistrerSucces(Long utilisateurId) {
        utilisateurRepository.findById(utilisateurId).ifPresent(u -> {
            if (u.getEchecsConnexion() != 0 || u.getVerrouilleJusquA() != null) {
                u.setEchecsConnexion(0);
                u.setVerrouilleJusquA(null);
            }
        });
    }

    /**
     * Les échecs sont comptés aussi ici (un jeton volé ne doit pas permettre de deviner le mot de passe) :
     * noRollbackFor garde le compteur même quand la requête est refusée.
     */
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public void changerMotDePasse(Long utilisateurId, ChangementMotDePasseRequest r) {
        Utilisateur u = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
        if (!passwordEncoder.matches(r.motDePasseActuel(), u.getMotDePasseHash())) {
            compterEchec(u);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mot de passe actuel incorrect");
        }
        if (r.nouveauMotDePasse().equals(r.motDePasseActuel())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le nouveau mot de passe doit être différent de l'actuel");
        }
        u.setMotDePasseHash(passwordEncoder.encode(r.nouveauMotDePasse()));
        u.setDoitChangerMotDePasse(false);
        u.setMotDePasseModifieLe(Instant.now(clock));
        u.setEchecsConnexion(0);
        u.setVerrouilleJusquA(null);
    }

    private void compterEchec(Utilisateur u) {
        Instant maintenant = Instant.now(clock);
        if (u.getVerrouilleJusquA() != null && maintenant.isBefore(u.getVerrouilleJusquA())) {
            return;
        }
        int echecs = u.getEchecsConnexion() + 1;
        if (echecs >= maxEchecs) {
            u.setVerrouilleJusquA(maintenant.plus(dureeVerrouillage));
            u.setEchecsConnexion(0);
        } else {
            u.setEchecsConnexion(echecs);
        }
    }
}
