package com.plateforme.scolaire.web;

import com.plateforme.scolaire.security.JwtService;
import com.plateforme.scolaire.security.UtilisateurPrincipal;
import com.plateforme.scolaire.service.SecuriteComptesService;
import com.plateforme.scolaire.web.dto.LoginRequest;
import com.plateforme.scolaire.web.dto.LoginResponse;
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

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest requete) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(requete.email(), requete.motDePasse()));
            UtilisateurPrincipal principal = (UtilisateurPrincipal) auth.getPrincipal();
            securiteComptesService.enregistrerSucces(principal.getId());
            return new LoginResponse(jwtService.generer(principal.getId()), "Bearer", principal.getId(),
                    principal.getUsername(), principal.isDoitChangerMotDePasse());
        } catch (BadCredentialsException e) {
            securiteComptesService.enregistrerEchec(requete.email());
            throw refus();
        } catch (AuthenticationException e) {
            // Compte verrouillé ou désactivé : même réponse, pour ne pas révéler l'état du compte
            throw refus();
        }
    }

    private static ResponseStatusException refus() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                "Identifiants invalides ou compte temporairement verrouillé");
    }
}
