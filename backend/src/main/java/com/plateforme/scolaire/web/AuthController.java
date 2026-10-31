package com.plateforme.scolaire.web;

import com.plateforme.scolaire.security.JwtService;
import com.plateforme.scolaire.security.UtilisateurPrincipal;
import com.plateforme.scolaire.service.DoubleAuthentificationService;
import com.plateforme.scolaire.service.ReinitialisationMotDePasseService;
import com.plateforme.scolaire.service.SecuriteComptesService;
import com.plateforme.scolaire.web.dto.DemandeReinitialisationRequest;
import com.plateforme.scolaire.web.dto.LoginRequest;
import com.plateforme.scolaire.web.dto.LoginResponse;
import com.plateforme.scolaire.web.dto.VerificationRequiseResponse;
import org.springframework.http.ResponseEntity;
import com.plateforme.scolaire.web.dto.ReinitialisationParJetonRequest;
import org.springframework.web.bind.annotation.ResponseStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final SecuriteComptesService securiteComptesService;
    private final ReinitialisationMotDePasseService reinitialisationService;
    private final DoubleAuthentificationService doubleAuthentification;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest requete) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(requete.email(), requete.motDePasse()));
            UtilisateurPrincipal principal = (UtilisateurPrincipal) auth.getPrincipal();
            if (doubleAuthentification.requise(principal)) {
                if (requete.code() == null || requete.code().isBlank()) {
                    doubleAuthentification.envoyerCode(principal.getId(), principal.getUsername());
                    return ResponseEntity.accepted().body(new VerificationRequiseResponse(true));
                }
                if (!doubleAuthentification.verifier(principal.getId(), requete.code())) {
                    throw refus();
                }
            }
            securiteComptesService.enregistrerSucces(principal.getId());
            return ResponseEntity.ok(new LoginResponse(jwtService.generer(principal.getId()), "Bearer", principal.getId(),
                    principal.getUsername(), principal.isDoitChangerMotDePasse()));
        } catch (BadCredentialsException e) {
            securiteComptesService.enregistrerEchec(requete.email());
            throw refus();
        } catch (AuthenticationException e) {
            // Compte verrouillé ou désactivé : même réponse, pour ne pas révéler l'état du compte
            throw refus();
        }
    }

    /** Toujours 204, que l'adresse existe ou non. */
    @PostMapping("/mot-de-passe-oublie")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void motDePasseOublie(@Valid @RequestBody DemandeReinitialisationRequest requete) {
        reinitialisationService.demander(requete.email());
    }

    @PostMapping("/reinitialisation")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reinitialiser(@Valid @RequestBody ReinitialisationParJetonRequest requete) {
        reinitialisationService.reinitialiser(requete.jeton(), requete.nouveauMotDePasse());
    }

    private static ResponseStatusException refus() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                "Identifiants invalides ou compte temporairement verrouillé");
    }
}
